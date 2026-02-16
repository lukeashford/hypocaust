package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.dto.TaskInitializationResult;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.repo.TaskExecutionRepository;
import com.example.hypocaust.service.events.EventService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

  private static final AnthropicChatModelSpec MESSAGE_GENERATION_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final TaskExecutionRepository taskExecutionRepository;
  private final VersionManagementService versionService;
  private final TodoService todoService;
  private final EventService eventService;
  private final ModelRegistry modelRegistry;

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

    // Create TaskExecution entity and transition to RUNNING synchronously
    final var taskExecution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task(task)
        .predecessorId(predecessorId)
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
    String commitMessage = generateCommitMessage(task);

    // Complete the task execution
    taskExecution.complete(commitMessage, delta);
    taskExecutionRepository.save(taskExecution);

    // Publish completion event
//    eventService.publish(
//        new TaskExecutionCompletedEvent(taskExecutionId, delta != null, commitMessage));
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
}
