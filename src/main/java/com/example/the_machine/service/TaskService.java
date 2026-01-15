package com.example.the_machine.service;

import com.example.the_machine.db.ProjectEntity;
import com.example.the_machine.dto.CreateTaskRequestDto;
import com.example.the_machine.dto.TaskResponseDto;
import com.example.the_machine.logging.ModelCallLogger;
import com.example.the_machine.operator.DecomposingOperator;
import com.example.the_machine.repo.ProjectRepository;
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
    final var project = projectRepository.save(new ProjectEntity());
    final var projectId = project.getId();
    log.info("Created project {} for task: {}", projectId, truncateTitle(task));

    // Kick off the decomposing operator asynchronously
    runExecutorService.submit(() -> executeTask(projectId, task));

    return TaskResponseDto.accepted(projectId);
  }

  private void executeTask(UUID projectId, String task) {
    log.info("Starting task execution for project: {}", projectId);

    // Reset call sequence counter for this task execution
    modelCallLogger.resetSequence();

    try {
      // Augment the task with the picture generation note
      final var augmentedTask = task + "\n\n" + PICTURE_GENERATION_NOTE;

      final var result = decomposingOperator.execute(Map.of("task", augmentedTask));

      if (result.ok()) {
        log.info("Task completed successfully for project: {}", projectId);
      } else {
        log.error("Task failed for project {}: {}", projectId, result.message());
      }
    } catch (Exception e) {
      log.error("Error executing task for project {}: {}", projectId, e.getMessage(), e);
    }
  }

  private String truncateTitle(String task) {
    return task.length() > 100 ? task.substring(0, 100) + "..." : task;
  }
}
