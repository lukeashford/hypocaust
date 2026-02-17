package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskInitializationResult;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock
  private ProjectService projectService;

  @Mock
  private Decomposer decomposer;

  @Mock
  private ExecutorService runExecutorService;

  @Mock
  private ModelCallLogger modelCallLogger;

  @Mock
  private TaskExecutionContextFactory contextFactory;

  @Mock
  private TaskExecutionLifecycleService lifecycleService;

  @InjectMocks
  private TaskService taskService;

  private UUID projectId;
  private UUID taskExecutionId;
  private UUID predecessorId;
  private UUID firstEventId;

  @BeforeEach
  void setUp() {
    projectId = UUID.randomUUID();
    taskExecutionId = UUID.randomUUID();
    predecessorId = UUID.randomUUID();
    firstEventId = UUID.randomUUID();
  }

  @Test
  void submitTask_validRequest_orchestratesCorrectly() {
    // Given
    String taskDescription = "test task";
    CreateTaskRequestDto request = new CreateTaskRequestDto(projectId, predecessorId,
        taskDescription);

    when(projectService.exists(projectId)).thenReturn(true);
    when(lifecycleService.startExecution(projectId, taskDescription, predecessorId))
        .thenReturn(
            new TaskInitializationResult(projectId, taskExecutionId, predecessorId, firstEventId));

    // Capture the lambda submitted to executor
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

    // When
    TaskResponseDto response = taskService.submitTask(request);

    // Then
    assertThat(response.projectId()).isEqualTo(projectId);
    assertThat(response.taskExecutionId()).isEqualTo(taskExecutionId);
    assertThat(response.firstEventId()).isEqualTo(firstEventId);
    assertThat(response.status()).isEqualTo("accepted");

    verify(projectService).exists(projectId);
    verify(lifecycleService).startExecution(projectId, taskDescription, predecessorId);
    verify(runExecutorService).submit(runnableCaptor.capture());

    // Verify background task logic (at least that it calls executeTask)
    // We can't easily verify the call to the private method directly, 
    // but we can verify what executeTask does if we invoke the captured runnable.

    // Preparation for executeTask verification
    TaskExecutionContext mockContext = mock(TaskExecutionContext.class);
    when(contextFactory.create(projectId, taskExecutionId, predecessorId)).thenReturn(mockContext);
    when(mockContext.getTaskExecutionId()).thenReturn(taskExecutionId);
    when(decomposer.execute(taskDescription)).thenReturn(DecomposerResult.success("done", null));

    // Execute the runnable
    runnableCaptor.getValue().run();

    verify(modelCallLogger).resetSequence();
    verify(decomposer).execute(taskDescription);
    verify(lifecycleService).commitExecution(eq(taskExecutionId), eq(projectId),
        eq(taskDescription), eq(mockContext));
  }

  @Test
  void submitTask_projectNotFound_returnsRejected() {
    // Given
    CreateTaskRequestDto request = new CreateTaskRequestDto(projectId, null, "task");
    when(projectService.exists(projectId)).thenReturn(false);

    // When
    TaskResponseDto response = taskService.submitTask(request);

    // Then
    assertThat(response.status()).isEqualTo("rejected");
    assertThat(response.message()).contains("Project not found");
  }

  @Test
  void executeTask_failure_callsFailExecution() {
    // Given
    String taskDescription = "fail task";
    TaskExecutionContext mockContext = mock(TaskExecutionContext.class);
    when(mockContext.getTaskExecutionId()).thenReturn(taskExecutionId);
    when(contextFactory.create(projectId, taskExecutionId, predecessorId)).thenReturn(mockContext);
    when(decomposer.execute(taskDescription)).thenReturn(DecomposerResult.failure("error"));

    // When
    taskService.executeTask(projectId, taskExecutionId, predecessorId, taskDescription);

    // Then
    verify(lifecycleService).failExecution(eq(taskExecutionId), eq("error"));
  }

  @Test
  void executeTask_exception_callsFailExecution() {
    // Given
    String taskDescription = "exception task";
    TaskExecutionContext mockContext = mock(TaskExecutionContext.class);
    when(mockContext.getTaskExecutionId()).thenReturn(taskExecutionId);
    when(contextFactory.create(projectId, taskExecutionId, predecessorId)).thenReturn(mockContext);
    when(decomposer.execute(taskDescription)).thenThrow(new RuntimeException("crash"));

    // When
    taskService.executeTask(projectId, taskExecutionId, predecessorId, taskDescription);

    // Then
    verify(lifecycleService).failExecution(eq(taskExecutionId), eq("crash"));
  }
}
