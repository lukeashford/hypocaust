package com.example.hypocaust.service;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.agent.TodoExecutor;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.staging.AnalyzedUpload;
import com.example.hypocaust.service.staging.StagingService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
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
        integrateStagedUploads(batchId, context);
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

  private static final String FALLBACK_NAME = "unknown";
  private static final String FALLBACK_TITLE = "Unknown Upload";
  private static final String FALLBACK_DESCRIPTION = "User-uploaded file (analysis unavailable)";

  private void integrateStagedUploads(UUID batchId, TaskExecutionContext context) {
    todoExecutor.execute("Integrating uploads...", () -> {
      List<AnalyzedUpload> uploads = stagingService.consumeBatch(batchId);

      for (AnalyzedUpload upload : uploads) {
        AnalysisResult result = upload.analysisResult();

        String name = resolveWithPriority(upload.clientName(),
            result != null ? result.name() : null,
            sanitizeFilename(upload.originalFilename()));
        String title = resolveWithPriority(upload.clientTitle(),
            result != null ? result.title() : null,
            upload.originalFilename());
        String description = resolveWithPriority(upload.clientDescription(),
            result != null ? result.description() : null,
            FALLBACK_DESCRIPTION);

        if (name == null) {
          name = FALLBACK_NAME;
        }
        if (title == null) {
          title = FALLBACK_TITLE;
        }

        context.getArtifacts().addManifested(name, title, description, upload.kind(),
            upload.storageKey(), upload.inlineContent(), upload.mimeType(),
            result != null ? result.enrichedMetadata() : null);
      }

      log.info("Integrated {} staged uploads into task execution", uploads.size());
      return DecomposerResult.success("Uploads integrated");
    });
  }

  private static String resolveWithPriority(String clientValue, String analysisValue,
      String fallbackValue) {
    if (clientValue != null && !clientValue.isBlank()) {
      return clientValue;
    }
    if (analysisValue != null && !analysisValue.isBlank()) {
      return analysisValue;
    }
    if (fallbackValue != null && !fallbackValue.isBlank()) {
      return fallbackValue;
    }
    return null;
  }

  private static String sanitizeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return null;
    }
    int dotIndex = filename.lastIndexOf('.');
    return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
  }
}
