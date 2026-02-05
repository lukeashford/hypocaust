package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextFactory;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.domain.event.TaskExecutionCompletedEvent;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.dto.CreateTaskRequestDto;
import com.example.hypocaust.dto.TaskResponseDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.operator.DecomposingOperator;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.repo.ProjectRepository;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.events.EventService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

  private static final AnthropicChatModelSpec MESSAGE_GENERATION_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ProjectRepository projectRepository;
  private final TaskExecutionRepository taskExecutionRepository;
  private final DecomposingOperator decomposingOperator;
  private final ExecutorService runExecutorService;
  private final ModelCallLogger modelCallLogger;
  private final EventService eventService;
  private final VersionManagementService versionService;
  private final TodoService todoService;
  private final TaskExecutionContextFactory contextFactory;
  private final ModelRegistry modelRegistry;

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
    if (!projectRepository.existsById(projectId)) {
      return TaskResponseDto.rejected("Project not found: " + projectId);
    }

    // Resolve predecessorId: use provided or find most recent completed
    // null if none exists (which would be fine for an empty project)
    UUID predecessorId = predecessorIdInput;
    if (predecessorId == null) {
      predecessorId = versionService.getMostRecentTaskExecutionId(projectId).orElse(null);
    }

    // Create TaskExecution entity
    final var taskExecution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task(task)
        .predecessorId(predecessorId)
        .build();
    taskExecutionRepository.save(taskExecution);
    final var taskExecutionId = taskExecution.getId();

    log.info("Created TaskExecution {} for project {} with predecessor {}",
        taskExecutionId, projectId, predecessorId);

    // Kick off execution asynchronously
    final UUID finalPredecessorId = predecessorId;
    runExecutorService.submit(
        () -> executeTask(projectId, taskExecutionId, finalPredecessorId, task));

    return TaskResponseDto.accepted(projectId, taskExecutionId);
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

    // Update status to RUNNING
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(
            () -> new IllegalStateException("TaskExecution not found: " + taskExecutionId));

    try {
      taskExecution.start();
      taskExecutionRepository.save(taskExecution);
      eventService.publish(new TaskExecutionStartedEvent(taskExecutionId));

      // Augment the task with the picture generation note
      final var augmentedTask = task + "\n\n" + PICTURE_GENERATION_NOTE;

      // Execute with null todoId (root level)
      final var result = decomposingOperator.execute(Map.of("task", augmentedTask));

      if (result.ok()) {
        commitExecution(taskExecutionId, projectId, task, context);
        log.info("Task completed successfully for project {}", projectId);
      } else {
        failExecution(taskExecutionId, projectId, result.message(), context.getArtifacts());
        log.error("Task failed for project {}: {}", projectId, result.message());
      }
    } catch (Exception e) {
      failExecution(taskExecutionId, projectId, e.getMessage(),
          context != null ? context.getArtifacts() : null);
      log.error("Error executing task for project {}: {}", projectId, e.getMessage(), e);
    } finally {
      // Always clear the context when done
      TaskExecutionContextHolder.clear();
    }
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
    String commitMessage = generateCommitMessage(task);

    // Complete the task execution
    taskExecution.complete(commitMessage, delta);
    taskExecutionRepository.save(taskExecution);

    // Publish completion event
    eventService.publish(
        new TaskExecutionCompletedEvent(taskExecutionId, delta != null, commitMessage));
  }

  /**
   * Fail a task execution. Publishes failure event. Pending artifacts are simply not persisted.
   *
   * @param taskExecutionId The TaskExecution to fail
   * @param projectId The project ID (for events)
   * @param errorMessage The error message describing the failure
   * @param artifacts The artifacts context (unused, pending changes are not persisted)
   */
  @Transactional
  public void failExecution(UUID taskExecutionId, UUID projectId, String errorMessage,
      ArtifactsContext artifacts) {
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

  /**
   * Generate a commit message summarizing the task using an LLM.
   */
  private String generateCommitMessage(String task) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(MESSAGE_GENERATION_MODEL))
          .build();

      String response = chatClient.prompt()
          .system("""
              Generate a brief commit message (1 sentence, max 100 chars) summarizing what was done.
              Focus on the outcome, not the process. Start with a verb like "Added", "Created", "Updated".
              """)
          .user("Task: " + task)
          .call()
          .content();

      if (response != null && !response.isBlank()) {
        // Truncate if too long
        return response.length() > 100 ? response.substring(0, 100) : response.trim();
      }
    } catch (Exception e) {
      log.warn("Failed to generate commit message, using default: {}", e.getMessage());
    }

    // Fallback
    return "Completed task";
  }

  private String truncateTitle(String task) {
    return task.length() > 100 ? task.substring(0, 100) + "..." : task;
  }
}
