package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.DecomposingOperator;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.events.EventService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling task submission and execution. Manages the TaskExecution lifecycle including
 * version control integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private static final String PICTURE_GENERATION_NOTE = """
      Note: This system currently only supports tasks related to brainstorming a story and generating a fitting picture.
      If this task is not about generating a picture, fail early and do nothing.
      """;

  private final ProjectService projectService;
  private final TaskExecutionRepository taskExecutionRepository;
  private final DecomposingOperator decomposingOperator;
  private final ExecutorService runExecutorService;
  private final ModelCallLogger modelCallLogger;
  private final EventService eventService;
  private final VersionManagementService versionService;
  private final TaskExecutionContextFactory contextFactory;
  private final TaskExecutionLifecycleService lifecycleService;

  @Transactional
  public TaskResponseDto submitTask(CreateTaskRequestDto request) {
    final var task = request.task();
    final var projectId = request.projectId();
    final var predecessorIdInput = request.predecessorId();

    if (task == null || task.isBlank()) {
      return TaskResponseDto.rejected("Task description is required");
    }

    if (projectId == null) {
      return TaskResponseDto.rejected("Project ID is required");
    }

    // Verify project exists
    if (!projectService.exists(projectId)) {
      return TaskResponseDto.rejected("Project not found: " + projectId);
    }

    // Resolve predecessorId: use provided or find most recent completed
    // null if none exists (which would be fine for an empty project)
    UUID predecessorId = Optional.ofNullable(predecessorIdInput)
        .orElseGet(() -> versionService.getMostRecentTaskExecutionId(projectId)
            .orElse(null));

    // Create TaskExecution entity and transition to RUNNING synchronously
    final var taskExecution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task(task)
        .predecessorId(predecessorId)
        .build();
    taskExecution.start();
    taskExecutionRepository.save(taskExecution);
    final var taskExecutionId = taskExecution.getId();

    // Publish the started event synchronously so the client receives its ID
    // before connecting to SSE — guarantees no events are lost
    final UUID firstEventId = eventService.publish(
        new TaskExecutionStartedEvent(taskExecutionId));

    log.info("Created TaskExecution {} for project {} with predecessor {}",
        taskExecutionId, projectId, predecessorId);

    // Kick off execution asynchronously
    final UUID finalPredecessorId = predecessorId;
    runExecutorService.submit(
        () -> executeTask(projectId, taskExecutionId, finalPredecessorId, task));

    return TaskResponseDto.accepted(projectId, taskExecutionId, firstEventId);
  }

  public void executeTask(UUID projectId, UUID taskExecutionId, UUID predecessorId, String task) {
    log.info("Starting task execution {} for project {}", taskExecutionId, projectId);

    // Create context
    TaskExecutionContext context = contextFactory.create(projectId, taskExecutionId,
        predecessorId);

    // Set the context for this thread
    TaskExecutionContextHolder.setContext(context);

    // Reset call sequence counter
    modelCallLogger.resetSequence();

    // Task is already in RUNNING status and the started event was published
    // synchronously during submitTask()
    try {
      // Augment the task with the picture generation note
      final var augmentedTask = task + "\n\n" + PICTURE_GENERATION_NOTE;

      // Execute with null todoId (root level)
      final var result = decomposingOperator.execute(Map.of("task", augmentedTask), null);

      if (result.ok()) {
        lifecycleService.commitExecution(taskExecutionId, projectId, task, context);
        log.info("Task completed successfully for project {}", projectId);
      } else {
        lifecycleService.failExecution(taskExecutionId, projectId, result.message(),
            context.getArtifacts());
        log.error("Task failed for project {}: {}", projectId, result.message());
      }
    } catch (Exception e) {
      lifecycleService.failExecution(taskExecutionId, projectId, e.getMessage(),
          context != null ? context.getArtifacts() : null);
      log.error("Error executing task for project {}: {}", projectId, e.getMessage(), e);
    } finally {
      // Always clear the context when done
      TaskExecutionContextHolder.clear();
    }
  }
}
