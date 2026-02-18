package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.domain.event.TaskExecutionCompletedEvent;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.dto.TaskInitializationResult;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.events.EventService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the transactional lifecycle transitions of a TaskExecution. Separated from TaskService to
 * avoid transactional self-invocation issues.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutionLifecycleService {

  private final TaskExecutionRepository taskExecutionRepository;
  private final VersionManagementService versionService;
  private final TodoService todoService;
  private final EventService eventService;
  private final NamingService namingService;
  private final WordingService wordingService;

  /**
   * Initialize a task execution. Transitions to RUNNING synchronously.
   *
   * @param projectId The project ID
   * @param task The task description
   * @param predecessorIdInput The optional predecessor ID
   * @return TaskInitializationResult containing all IDs needed for orchestration
   */
  @Transactional
  public TaskInitializationResult startExecution(UUID projectId, String task,
      UUID predecessorIdInput) {
    // Resolve predecessorId: use provided or find most recent completed
    UUID predecessorId = Optional.ofNullable(predecessorIdInput)
        .orElseGet(() -> versionService.getMostRecentTaskExecutionId(projectId)
            .orElse(null));

    // Generate execution name upfront so the entity is named from creation
    Set<String> existingNames = taskExecutionRepository.findAllNamesByProjectId(projectId);
    String name = namingService.generateExecutionName(task, existingNames);

    // Create TaskExecution entity and transition to RUNNING synchronously
    final var taskExecution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task(task)
        .predecessorId(predecessorId)
        .name(name)
        .build();
    taskExecution.start();
    taskExecutionRepository.save(taskExecution);
    final var taskExecutionId = taskExecution.getId();

    // Publish the started event synchronously
    final UUID firstEventId = eventService.publish(
        new TaskExecutionStartedEvent(taskExecutionId));

    log.info("Created TaskExecution {} for project {} with predecessor {}",
        taskExecutionId, projectId, predecessorId);

    return new TaskInitializationResult(projectId, taskExecutionId, predecessorId, firstEventId);
  }

  /**
   * Commit a successful task execution. Materializes pending artifacts, generates commit message,
   * and publishes completion event.
   *
   * @param taskExecutionId The TaskExecution to commit
   * @param projectId The project ID (for events)
   * @param task The original task description (for message generation)
   * @param context The execution context containing artifacts and todos
   */
  @Transactional
  public void commitExecution(UUID taskExecutionId, UUID projectId, String task,
      TaskExecutionContext context) {
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(
            () -> new IllegalStateException("TaskExecution not found: " + taskExecutionId));

    ArtifactsContext artifacts = context.getArtifacts();

    // Materialize artifacts and get delta
    TaskExecutionDelta delta = versionService.materialize(artifacts.getChangelist(),
        taskExecutionId,
        projectId);

    // Materialize todos
    todoService.materialize(context.getTodos().getList(), taskExecutionId);

    // Generate commit message
    String commitMessage = wordingService.generateCommitMessage(task);

    // Complete the task execution
    taskExecution.complete(commitMessage, delta);
    taskExecutionRepository.save(taskExecution);

    // Publish completion event
    eventService.publish(
        new TaskExecutionCompletedEvent(taskExecutionId, delta != null, taskExecution.getName(),
            commitMessage));
  }

  /**
   * Fail a task execution. Publishes failure event. Pending artifacts are simply not persisted.
   *
   * @param taskExecutionId The TaskExecution to fail
   * @param errorMessage The error message describing the failure
   */
  @Transactional
  public void failExecution(UUID taskExecutionId, String errorMessage) {
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(
            () -> new IllegalStateException("TaskExecution not found: " + taskExecutionId));

    // Pending changes are simply not persisted - no explicit discard needed

    // Fail the task execution with error message as commitMessage
    taskExecution.fail(errorMessage);
    taskExecutionRepository.save(taskExecution);

    // Publish failure event
    eventService.publish(new TaskExecutionFailedEvent(taskExecutionId, errorMessage));
  }
}
