package com.example.hypocaust.service;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.agent.TodoExecutor;
import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.repo.ArtifactRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
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

  private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
  // 4 min > 3 min analysis timeout — guarantees analysis either completes or times out first
  private static final Duration MAX_ANALYSIS_WAIT = Duration.ofMinutes(4);

  private final ProjectService projectService;
  private final Decomposer decomposer;
  private final ExecutorService runExecutorService;
  private final ModelCallLogger modelCallLogger;
  private final TaskExecutionContextFactory contextFactory;
  private final TaskExecutionLifecycleService lifecycleService;
  private final TodoExecutor todoExecutor;
  private final ArtifactRepository artifactRepository;
  private final ArtifactAnalysisService artifactAnalysisService;

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
      // Await any pending upload analyses before starting the actual task
      awaitPendingAnalysesIfNeeded(projectId);

      // Deterministic label: truncate task at 80 chars
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
      // Always clear the context when done
      TaskExecutionContextHolder.clear();
    }
  }

  private void awaitPendingAnalysesIfNeeded(UUID projectId) {
    List<ArtifactEntity> pendingUploads = artifactRepository.findByProjectIdAndStatus(
        projectId, ArtifactStatus.UPLOADED);

    if (pendingUploads.isEmpty()) {
      return;
    }

    log.info("Found {} pending upload analyses for project {}", pendingUploads.size(), projectId);

    todoExecutor.execute("Analyzing uploads...", () -> {
      awaitPendingAnalyses(pendingUploads);
      return DecomposerResult.success("Uploads analyzed");
    });
  }

  private void awaitPendingAnalyses(List<ArtifactEntity> pending) {
    Instant deadline = Instant.now().plus(MAX_ANALYSIS_WAIT);
    Set<UUID> remaining = pending.stream()
        .map(ArtifactEntity::getId)
        .collect(Collectors.toCollection(HashSet::new));

    while (!remaining.isEmpty() && Instant.now().isBefore(deadline)) {
      List<ArtifactEntity> current = artifactRepository.findAllById(remaining);

      for (ArtifactEntity entity : current) {
        if (entity.getStatus() != ArtifactStatus.UPLOADED) {
          remaining.remove(entity.getId());
        }
      }

      // Also remove any that were deleted while we waited
      Set<UUID> foundIds = current.stream().map(ArtifactEntity::getId).collect(Collectors.toSet());
      remaining.retainAll(foundIds);

      if (remaining.isEmpty()) {
        break;
      }

      try {
        Thread.sleep(POLL_INTERVAL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    if (!remaining.isEmpty()) {
      log.warn("Analysis did not complete in time for {} artifact(s), applying fallback",
          remaining.size());
      for (UUID id : remaining) {
        artifactAnalysisService.forceComplete(id);
      }
    }
  }
}
