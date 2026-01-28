package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionContextConfig;
import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.domain.event.ArtifactRemovedEvent;
import com.example.hypocaust.domain.event.ArtifactUpdatedEvent;
import com.example.hypocaust.domain.event.TaskExecutionCompletedEvent;
import com.example.hypocaust.domain.event.TaskExecutionFailedEvent;
import com.example.hypocaust.domain.event.TaskExecutionStartedEvent;
import com.example.hypocaust.domain.event.TaskProgressUpdatedEvent;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling task submission and execution.
 * Manages the TaskExecution lifecycle including version control integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

  private static final String PICTURE_GENERATION_NOTE = """
      Note: This system currently only supports tasks related to brainstorming a story and generating a fitting picture.
      If this task is not about generating a picture, fail early and do nothing.
      """;

  private static final AnthropicChatModelSpec NAME_GENERATION_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ProjectRepository projectRepository;
  private final TaskExecutionRepository taskExecutionRepository;
  private final DecomposingOperator decomposingOperator;
  private final ExecutorService runExecutorService;
  private final ModelCallLogger modelCallLogger;
  private final EventService eventService;
  private final ArtifactVersionManagementService versionService;
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
    UUID predecessorId = predecessorIdInput;
    if (predecessorId == null) {
      predecessorId = versionService.getMostRecentTaskExecution(projectId)
          .map(TaskExecutionEntity::getId)
          .orElse(null);
    }

    // Create TaskExecution entity
    final var taskExecution = TaskExecutionEntity.builder()
        .projectId(projectId)
        .task(task)
        .status(TaskExecutionEntity.Status.QUEUED)
        .predecessorId(predecessorId)
        .build();
    taskExecutionRepository.save(taskExecution);
    final var taskExecutionId = taskExecution.getId();

    log.info("Created TaskExecution {} for project {} with predecessor {}",
        taskExecutionId, projectId, predecessorId);

    // Kick off execution asynchronously
    final UUID finalPredecessorId = predecessorId;
    runExecutorService.submit(() -> executeTask(projectId, taskExecutionId, finalPredecessorId, task));

    return TaskResponseDto.accepted(projectId, taskExecutionId);
  }

  public void executeTask(UUID projectId, UUID taskExecutionId, UUID predecessorId, String task) {
    log.info("Starting task execution {} for project {}", taskExecutionId, projectId);

    // Build context configuration with all callbacks
    TaskExecutionContextConfig config = TaskExecutionContextConfig.builder()
        .onArtifactAdded(data -> eventService.publish(new ArtifactAddedEvent(projectId,
            data.name(), data.kind(), data.description(),
            data.externalUrl(), data.inlineContent(), data.metadata())))
        .onArtifactUpdated(data -> eventService.publish(new ArtifactUpdatedEvent(projectId,
            data.name(), data.description(),
            data.externalUrl(), data.inlineContent(), data.metadata())))
        .onArtifactRemoved(data -> eventService.publish(new ArtifactRemovedEvent(projectId, data.name())))
        .onTaskProgressUpdated(taskTree -> eventService.publish(new TaskProgressUpdatedEvent(projectId, taskTree)))
        .artifactExistsChecker(name -> predecessorId != null
            && versionService.artifactExistsAtTaskExecution(predecessorId, name))
        .artifactKindGetter(name -> predecessorId == null
            ? Optional.empty()
            : versionService.getArtifactKindAtTaskExecution(predecessorId, name))
        .artifactNamesGetter(unused -> {
          if (predecessorId == null) {
            return Set.of();
          }
          Set<String> names = new HashSet<>();
          for (ArtifactEntity artifact : versionService.getArtifactsAtTaskExecution(predecessorId)) {
            if (!artifact.isDeleted()) {
              names.add(artifact.getName());
            }
          }
          return names;
        })
        .nameGenerator(request -> generateArtifactName(request.description(), request.existingNames()))
        .currentArtifactsGetter(unused -> predecessorId == null
            ? java.util.List.of()
            : versionService.getArtifactsAtTaskExecution(predecessorId))
        .build();

    // Create and configure context
    TaskExecutionContext context = new TaskExecutionContext(projectId, taskExecutionId, predecessorId);
    config.applyTo(context);

    // Set the context for this thread
    TaskExecutionContextHolder.setContext(context);

    // Reset call sequence counter
    modelCallLogger.resetSequence();

    // Update status to RUNNING
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(() -> new IllegalStateException("TaskExecution not found: " + taskExecutionId));

    try {
      taskExecution.start();
      taskExecutionRepository.save(taskExecution);
      eventService.publish(new TaskExecutionStartedEvent(projectId));

      // Augment the task with the picture generation note
      final var augmentedTask = task + "\n\n" + PICTURE_GENERATION_NOTE;

      // Execute with root todoPath "0"
      final var result = decomposingOperator.execute(Map.of("task", augmentedTask), "0");

      if (result.ok()) {
        // Complete the TaskExecution with pending changes
        TaskExecutionEntity completedExecution = versionService.complete(taskExecutionId, task, context.getPending());

        boolean hasChanges = context.getPending().hasChanges();
        // Use the commit message from the completed execution (already generated in complete())
        String commitMessage = hasChanges ? completedExecution.getCommitMessage() : null;

        eventService.publish(new TaskExecutionCompletedEvent(projectId, hasChanges, commitMessage));
        log.info("Task completed successfully for project {}", projectId);
      } else {
        taskExecution.fail(result.message());
        taskExecutionRepository.save(taskExecution);
        eventService.publish(new TaskExecutionFailedEvent(projectId, result.message()));
        log.error("Task failed for project {}: {}", projectId, result.message());
      }
    } catch (Exception e) {
      taskExecution.fail(e.getMessage());
      taskExecutionRepository.save(taskExecution);
      eventService.publish(new TaskExecutionFailedEvent(projectId, e.getMessage()));
      log.error("Error executing task for project {}: {}", projectId, e.getMessage(), e);
    } finally {
      // Always clear the context when done
      TaskExecutionContextHolder.clear();
    }
  }

  /**
   * Generate a unique artifact name from description using a small LLM.
   */
  private String generateArtifactName(String description, Set<String> existingNames) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(NAME_GENERATION_MODEL))
          .build();

      StringBuilder prompt = new StringBuilder();
      prompt.append("Generate a short, snake_case artifact name for: ").append(description);
      if (!existingNames.isEmpty()) {
        prompt.append("\n\nThe following names are already taken, choose a different one: ");
        prompt.append(String.join(", ", existingNames));
      }

      String response = chatClient.prompt()
          .system("""
              Generate a short snake_case name for an artifact (max 30 chars).
              Use only lowercase letters, numbers, and underscores.
              Reply with ONLY the name, nothing else.
              Examples: hero_portrait, forest_background, main_script
              """)
          .user(prompt.toString())
          .call()
          .content();

      if (response != null && !response.isBlank()) {
        String name = response.trim().toLowerCase()
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");

        // Truncate if too long
        if (name.length() > 30) {
          name = name.substring(0, 30);
        }

        // If name is still taken, append a number
        String baseName = name;
        int counter = 2;
        while (existingNames.contains(name)) {
          name = baseName + "_" + counter;
          counter++;
        }

        return name;
      }
    } catch (Exception e) {
      log.warn("Failed to generate artifact name via LLM: {}", e.getMessage());
    }

    // Fallback: generate from description
    String name = description.toLowerCase()
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_|_$", "");
    if (name.length() > 30) {
      name = name.substring(0, 30);
    }

    // If name is taken, append a number
    String baseName = name;
    int counter = 2;
    while (existingNames.contains(name)) {
      name = baseName + "_" + counter;
      counter++;
    }

    return name;
  }

  private String truncateTitle(String task) {
    return task.length() > 100 ? task.substring(0, 100) + "..." : task;
  }
}
