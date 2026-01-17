package com.example.hypocaust.service;

import com.example.hypocaust.db.ProjectEntity;
import com.example.hypocaust.db.RunEntity;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.DecomposingOperator;
import com.example.hypocaust.operator.RunContextHolder;
import com.example.hypocaust.repo.ProjectRepository;
import com.example.hypocaust.repo.RunRepository;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling task submission and execution.
 * Currently only supports picture generation tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private static final String PICTURE_GENERATION_NOTE = """
      Note: This system currently only supports tasks related to brainstorming a story and generating a fitting picture.
      If this task is not about generating a picture, fail early and do nothing.
      """;

  private final ProjectRepository projectRepository;
  private final RunRepository runRepository;
  private final DecomposingOperator decomposingOperator;
  private final ExecutorService runExecutorService;
  private final ModelCallLogger modelCallLogger;

  @Transactional
  public TaskResponseDto submitTask(CreateTaskRequestDto request) {
    final var task = request.task();
    
    if (task == null || task.isBlank()) {
      return TaskResponseDto.rejected("Task description is required");
    }

    // Create a new project
    final var project = projectRepository.saveAndFlush(new ProjectEntity());
    final var projectId = project.getId();
    log.info("Created project {} for task: {}", projectId, truncateTitle(task));

    // Kick off the decomposing operator asynchronously
    runExecutorService.submit(() -> executeTask(projectId, task));

    return TaskResponseDto.accepted(projectId);
  }

  @Transactional
  public void executeTask(UUID projectId, String task) {
    log.info("Starting task execution for project: {}", projectId);

    // Create and persist a run entity
    final var run = RunEntity.builder()
        .projectId(projectId)
        .task(task)
        .status(RunEntity.Status.QUEUED)
        .build();
    runRepository.save(run);
    final var runId = run.getId();

    log.info("Created run {} for project: {}", runId, projectId);

    // Set the run context for this thread
    RunContextHolder.setContext(projectId, runId);

    // Reset call sequence counter for this task execution
    modelCallLogger.resetSequence();

    try {
      // Start the run
      run.start();
      runRepository.save(run);

      // Augment the task with the picture generation note
      final var augmentedTask = task + "\n\n" + PICTURE_GENERATION_NOTE;

      final var result = decomposingOperator.execute(Map.of("task", augmentedTask));

      if (result.ok()) {
        run.complete("Task completed successfully");
        runRepository.save(run);
        log.info("Task completed successfully for project: {}", projectId);
      } else {
        run.fail(result.message());
        runRepository.save(run);
        log.error("Task failed for project {}: {}", projectId, result.message());
      }
    } catch (Exception e) {
      run.fail(e.getMessage());
      runRepository.save(run);
      log.error("Error executing task for project {}: {}", projectId, e.getMessage(), e);
    } finally {
      // Always clear the context when done
      RunContextHolder.clear();
    }
  }

  private String truncateTitle(String task) {
    return task.length() > 100 ? task.substring(0, 100) + "..." : task;
  }
}
