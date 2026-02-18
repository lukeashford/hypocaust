package com.example.hypocaust.service;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.agent.TodoExecutor;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for handling task submission and execution. Manages the TaskExecution lifecycle including
 * version control integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private final ProjectService projectService;
  private final Decomposer decomposer;
  private final ExecutorService runExecutorService;
  private final ModelCallLogger modelCallLogger;
  private final TaskExecutionContextFactory contextFactory;
  private final TaskExecutionLifecycleService lifecycleService;
  private final TodoExecutor todoExecutor;
  private final WordingService wordingService;

  public TaskResponseDto submitTask(CreateTaskRequestDto request) {
    final var task = request.task();
    final var projectId = request.projectId();

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

    // Atomic state transition
    // This call starts and finishes its own transaction.
    var init = lifecycleService.startExecution(projectId, task, request.predecessorId());

    // Kick off execution asynchronously
    // Now we are GUARANTEED that the database transaction has committed.
    runExecutorService.submit(
        () -> executeTask(init.projectId(), init.taskExecutionId(), init.predecessorId(),
            init.name(), task));

    return TaskResponseDto.accepted(init.projectId(), init.taskExecutionId(), init.firstEventId());
  }

  public void executeTask(UUID projectId, UUID taskExecutionId, UUID predecessorId, String name,
      String task) {
    log.info("Starting task execution {} for project {}", taskExecutionId, projectId);

    // Create context
    TaskExecutionContext context = contextFactory.create(projectId, taskExecutionId,
        predecessorId, name);

    // Set the context for this thread
    TaskExecutionContextHolder.setContext(context);

    // Reset call sequence counter
    modelCallLogger.resetSequence();

    // Task is already in RUNNING status and the started event was published
    // synchronously during lifecycleService.startExecution()
    try {
      // Generate a concise label for the root todo and execute within the todo lifecycle
      String rootLabel = wordingService.generateTodoWording(task);
      var result = todoExecutor.execute(rootLabel, () -> decomposer.execute(task));

      if (result.success()) {
        lifecycleService.commitExecution(taskExecutionId, projectId, task, context);
        log.info("Task completed successfully for project {}", projectId);
      } else {
        lifecycleService.failExecution(taskExecutionId, result.errorMessage());
        log.error("Task failed for project {}: {}", projectId, result.errorMessage());
      }
    } catch (Exception e) {
      lifecycleService.failExecution(taskExecutionId, e.getMessage());
      log.error("Error executing task for project {}: {}", projectId, e.getMessage(), e);
    } finally {
      // Always clear the context when done
      TaskExecutionContextHolder.clear();
    }
  }
}
