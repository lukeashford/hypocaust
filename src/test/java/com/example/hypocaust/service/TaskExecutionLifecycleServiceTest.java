package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.TaskExecutionStatus;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.dto.TaskInitializationResult;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.events.EventService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@Import(TaskExecutionLifecycleService.class)
class TaskExecutionLifecycleServiceTest {

  @Autowired
  private TaskExecutionLifecycleService lifecycleService;

  @Autowired
  private TaskExecutionRepository taskExecutionRepository;

  @MockitoBean
  private VersionManagementService versionService;

  @MockitoBean
  private TodoService todoService;

  @MockitoBean
  private EventService eventService;

  @Test
  void startExecution_withPredecessorId_createsCorrectExecution() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID predecessorId = UUID.randomUUID();
    String task = "test task";
    UUID eventId = UUID.randomUUID();

    when(eventService.publish(any(TaskExecutionStartedEvent.class))).thenReturn(eventId);

    // When — execution name is now deterministic: sanitize("test task", 50) = "test_task"
    TaskInitializationResult result = lifecycleService.startExecution(projectId, task,
        predecessorId);

    // Then
    assertThat(result.projectId()).isEqualTo(projectId);
    assertThat(result.predecessorId()).isEqualTo(predecessorId);
    assertThat(result.firstEventId()).isEqualTo(eventId);
    assertThat(result.name()).isEqualTo("test_task");

    Optional<TaskExecutionEntity> saved = taskExecutionRepository.findById(
        result.taskExecutionId());
    assertThat(saved).isPresent();
    assertThat(saved.get().getProjectId()).isEqualTo(projectId);
    assertThat(saved.get().getTask()).isEqualTo(task);
    assertThat(saved.get().getPredecessorId()).isEqualTo(predecessorId);
    assertThat(saved.get().getStatus()).isEqualTo(TaskExecutionStatus.RUNNING);
    assertThat(saved.get().getStartedAt()).isNotNull();

    verify(eventService).publish(any(TaskExecutionStartedEvent.class));
  }

  @Test
  void startExecution_withoutPredecessorId_resolvesFromVersionService() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID resolvedPredecessorId = UUID.randomUUID();
    String task = "test task";

    when(versionService.getMostRecentTaskExecutionId(projectId))
        .thenReturn(Optional.of(resolvedPredecessorId));
    when(eventService.publish(any(TaskExecutionStartedEvent.class))).thenReturn(UUID.randomUUID());

    // When
    TaskInitializationResult result = lifecycleService.startExecution(projectId, task, null);

    // Then
    assertThat(result.predecessorId()).isEqualTo(resolvedPredecessorId);

    TaskExecutionEntity saved = taskExecutionRepository.findById(result.taskExecutionId())
        .orElseThrow();
    assertThat(saved.getPredecessorId()).isEqualTo(resolvedPredecessorId);
  }

  @Test
  void commitExecution_validExecution_transitionsToCompleted() {
    // Given
    UUID projectId = UUID.randomUUID();
    TaskExecutionEntity execution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task("test task")
        .status(TaskExecutionStatus.RUNNING)
        .build();
    execution = taskExecutionRepository.save(execution);
    UUID executionId = execution.getId();

    com.example.hypocaust.domain.TaskExecutionContext context = org.mockito.Mockito.mock(
        com.example.hypocaust.domain.TaskExecutionContext.class);
    com.example.hypocaust.domain.ArtifactsContext artifacts = org.mockito.Mockito.mock(
        com.example.hypocaust.domain.ArtifactsContext.class);
    com.example.hypocaust.domain.TodosContext todosContext = org.mockito.Mockito.mock(
        com.example.hypocaust.domain.TodosContext.class);
    com.example.hypocaust.domain.TodoList todoList = org.mockito.Mockito.mock(
        com.example.hypocaust.domain.TodoList.class);
    com.example.hypocaust.domain.Changelist changelist = new com.example.hypocaust.domain.Changelist();

    when(context.getArtifacts()).thenReturn(artifacts);
    when(artifacts.getChangelist()).thenReturn(changelist);
    when(context.getTodos()).thenReturn(todosContext);
    when(todosContext.getList()).thenReturn(todoList);
    when(versionService.persist(any(), any(), any()))
        .thenReturn(new com.example.hypocaust.domain.TaskExecutionDelta());

    // When — commit message is now the task itself
    lifecycleService.commitExecution(executionId, projectId, "test task", context);

    // Then
    TaskExecutionEntity updated = taskExecutionRepository.findById(executionId).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(TaskExecutionStatus.COMPLETED);
    assertThat(updated.getCompletedAt()).isNotNull();
    assertThat(updated.getCommitMessage()).isEqualTo("test task");

    verify(versionService).persist(any(), eq(executionId), eq(projectId));
    verify(todoService).materialize(any(), eq(executionId));
    verify(eventService).publish(
        any(com.example.hypocaust.domain.event.TaskExecutionCompletedEvent.class));
  }

  @Test
  void failExecution_validExecution_transitionsToFailed() {
    // Given
    UUID projectId = UUID.randomUUID();
    TaskExecutionEntity execution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task("test task")
        .status(TaskExecutionStatus.RUNNING)
        .build();
    execution = taskExecutionRepository.save(execution);
    UUID executionId = execution.getId();

    // When
    lifecycleService.failExecution(executionId, "error message");

    // Then
    TaskExecutionEntity updated = taskExecutionRepository.findById(executionId).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(TaskExecutionStatus.FAILED);
    assertThat(updated.getCommitMessage()).isEqualTo("error message");
    assertThat(updated.getCompletedAt()).isNotNull();

    verify(eventService).publish(
        any(com.example.hypocaust.domain.event.TaskExecutionFailedEvent.class));
  }

  @Test
  void startExecution_withLongTaskNameAndExistingTask_shouldNotExceed50Chars() {
    // Given
    UUID projectId = UUID.randomUUID();
    String longTask = "This is a very long task name that will definitely be more than fifty characters long";

    when(eventService.publish(any())).thenReturn(UUID.randomUUID());

    // First execution
    var result1 = lifecycleService.startExecution(projectId, longTask, null);
    assertThat(result1.name().length()).isLessThanOrEqualTo(50);

    // Second execution with same task (should trigger appendCounterIfExists)
    var result2 = lifecycleService.startExecution(projectId, longTask, null);
    String name2 = result2.name();

    // Then
    assertThat(name2.length()).as("Name '%s' should not exceed 50 characters", name2)
        .isLessThanOrEqualTo(50);
    assertThat(name2).isNotEqualTo(result1.name());

    Optional<TaskExecutionEntity> saved = taskExecutionRepository.findById(
        result2.taskExecutionId());
    assertThat(saved).isPresent();
    assertThat(saved.get().getName()).isEqualTo(name2);
  }
}
