package com.example.hypocaust.service;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.agent.TodoExecutor;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.service.staging.StagingService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
  private final StagingService stagingService;

  public TaskResponseDto submitTask(CreateTaskRequestDto request) {
    final var task = request.task();
    final var projectId = request.projectId();

    if (task == null || task.isBlank()) {
      return TaskResponseDto.rejected("Task description is required");
    }

    if (projectId == null) {
      return TaskResponseDto.rejected("Project ID is required");
    }

    if (!projectService.exists(projectId)) {
      return TaskResponseDto.rejected("Project not found: " + projectId);
    }

    var init = lifecycleService.startExecution(projectId, task, request.predecessorId());

    runExecutorService.submit(
        () -> executeTask(init.projectId(), init.taskExecutionId(), init.predecessorId(),
            init.name(), task, request.batchId()));

    return TaskResponseDto.accepted(init.projectId(), init.taskExecutionId(), init.firstEventId());
  }

  public void executeTask(UUID projectId, UUID taskExecutionId, UUID predecessorId, String name,
      String task, UUID batchId) {
    log.info("Starting task execution {} for project {}", taskExecutionId, projectId);

    TaskExecutionContext context = contextFactory.create(projectId, taskExecutionId,
        predecessorId, name);

    TaskExecutionContextHolder.setContext(context);
    modelCallLogger.resetSequence();

    try {
      if (batchId != null) {
        integrateStagedUploads(batchId, projectId, context);
      }

      String rootLabel = task.length() <= 80 ? task : task.substring(0, 77) + "...";
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
      TaskExecutionContextHolder.clear();
    }
  }

  private void integrateStagedUploads(UUID batchId, UUID projectId,
      TaskExecutionContext context) {
    todoExecutor.execute("Integrating uploads...", () -> {
      Set<String> takenNames = context.getArtifacts().getAllWithChanges().stream()
          .map(Artifact::name)
          .collect(Collectors.toSet());
      Set<String> takenTitles = context.getArtifacts().getAllWithChanges().stream()
          .map(Artifact::title)
          .collect(Collectors.toSet());

      List<Artifact> artifacts = stagingService.consumeBatch(batchId, projectId,
          takenNames, takenTitles);

      for (Artifact artifact : artifacts) {
        context.getArtifacts().getChangelist().addArtifact(artifact);
      }

      log.info("Integrated {} staged uploads into task execution", artifacts.size());
      return DecomposerResult.success("Uploads integrated");
    });
  }
}
