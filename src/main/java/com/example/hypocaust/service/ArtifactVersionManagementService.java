package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.PendingArtifact;
import com.example.hypocaust.domain.PendingChanges;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.repo.ArtifactRepository;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central service for all version control operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactVersionManagementService {

  private static final AnthropicChatModelSpec MESSAGE_GENERATION_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final TaskExecutionRepository taskExecutionRepository;
  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;
  private final ModelRegistry modelRegistry;

  // === TaskExecution Completion Operations ===

  /**
   * Complete a TaskExecution with its pending changes.
   * Called by TaskService at TaskExecution completion.
   *
   * @param taskExecutionId The TaskExecution to complete
   * @param task            The original task (for message generation)
   * @param pending         The accumulated changes
   * @return The completed TaskExecution, with delta if there were changes
   */
  @Transactional
  public TaskExecutionEntity complete(UUID taskExecutionId, String task, PendingChanges pending) {
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(() -> new IllegalArgumentException("TaskExecution not found: " + taskExecutionId));

    if (!pending.hasChanges()) {
      // No changes - just complete without delta
      taskExecution.complete("Task completed successfully");
      return taskExecutionRepository.save(taskExecution);
    }

    // Generate commit message
    String commitMessage = generateMessage(task);

    // Materialize artifacts
    for (PendingArtifact artifact : pending.getActivePending()) {
      materializeArtifact(artifact, taskExecutionId, taskExecution.getProjectId());
    }

    // Build delta
    TaskExecutionDelta delta = pending.toTaskExecutionDelta();

    // Complete with changes
    taskExecution.complete("Task completed successfully", commitMessage, delta);
    return taskExecutionRepository.save(taskExecution);
  }

  /**
   * Generate a summary message from a task using a small LLM.
   */
  public String generateMessage(String task) {
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

  // === Artifact Resolution ===

  /**
   * Get the current artifacts for a TaskExecution by traversing history.
   * Walks from the TaskExecution back through predecessors, progressively building state.
   */
  public List<ArtifactEntity> getArtifactsAtTaskExecution(UUID taskExecutionId) {
    // Build TaskExecution chain from root to target
    List<TaskExecutionEntity> chain = new ArrayList<>();
    UUID current = taskExecutionId;

    while (current != null) {
      TaskExecutionEntity taskExecution = taskExecutionRepository.findById(current)
          .orElseThrow(() -> new IllegalArgumentException("TaskExecution not found: " + current));
      chain.add(0, taskExecution);  // Prepend to get oldest first
      current = taskExecution.getPredecessorId();
    }

    // Replay deltas to build current state
    Map<String, ArtifactEntity> state = new HashMap<>();

    for (TaskExecutionEntity taskExecution : chain) {
      TaskExecutionDelta delta = taskExecution.getDelta();
      if (delta == null) {
        continue;  // No changes in this TaskExecution
      }

      // Add new artifacts
      for (ArtifactChange added : delta.added()) {
        ArtifactEntity artifact = artifactRepository
            .findByTaskExecutionIdAndName(taskExecution.getId(), added.name())
            .orElse(null);
        if (artifact != null) {
          state.put(added.name(), artifact);
        }
      }

      // Apply edits (new versions)
      for (ArtifactChange edited : delta.edited()) {
        ArtifactEntity artifact = artifactRepository
            .findByTaskExecutionIdAndName(taskExecution.getId(), edited.name())
            .orElse(null);
        if (artifact != null) {
          state.put(edited.name(), artifact);
        }
      }

      // Remove deleted
      for (String deleted : delta.deleted()) {
        state.remove(deleted);
      }
    }

    return new ArrayList<>(state.values());
  }

  /**
   * Get artifacts for a TaskExecution.
   * If TaskExecution is completed with changes: return artifacts at that snapshot.
   * If TaskExecution is in progress: return predecessor artifacts.
   */
  public List<ArtifactEntity> getArtifactsForTaskExecution(UUID taskExecutionId) {
    TaskExecutionEntity taskExecution = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(() -> new IllegalArgumentException("TaskExecution not found: " + taskExecutionId));

    if (taskExecution.getStatus() == TaskExecutionEntity.Status.COMPLETED
        && taskExecution.getDelta() != null) {
      // Return artifacts at this snapshot
      return getArtifactsAtTaskExecution(taskExecutionId);
    }

    // Return predecessor artifacts
    if (taskExecution.getPredecessorId() != null) {
      return getArtifactsAtTaskExecution(taskExecution.getPredecessorId());
    }

    // No predecessor - return empty
    return List.of();
  }

  /**
   * Get the most recent completed TaskExecution for a project (with or without changes).
   */
  public Optional<TaskExecutionEntity> getMostRecentTaskExecution(UUID projectId) {
    return taskExecutionRepository.findMostRecentCompletedByProjectId(projectId);
  }

  /**
   * Get full TaskExecution history for a project.
   */
  public List<TaskExecutionEntity> getTaskExecutionHistory(UUID projectId) {
    return taskExecutionRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
  }

  /**
   * Get all versions of an artifact by name (across TaskExecutions).
   */
  public List<ArtifactEntity> getVersionHistory(UUID projectId, String artifactName) {
    return artifactRepository.findByProjectIdAndName(projectId, artifactName);
  }

  // === Used by completion process ===

  /**
   * Download and store a pending artifact.
   * Called during completion for artifacts that have external URLs.
   */
  @Transactional
  public void materializeArtifact(PendingArtifact pending, UUID taskExecutionId, UUID projectId) {
    String storageKey = null;

    // Download external URL if present
    if (pending.externalUrl() != null && !pending.externalUrl().isBlank()) {
      try {
        byte[] data;
        try (var stream = new URI(pending.externalUrl()).toURL().openStream()) {
          data = stream.readAllBytes();
        }

        String mimeType = pending.kind() == ArtifactEntity.Kind.IMAGE ? "image/png" : "application/octet-stream";
        storageKey = storageService.store(data, mimeType, pending.name());
        log.info("Downloaded and stored artifact {} with key {}", pending.name(), storageKey);
      } catch (Exception e) {
        log.error("Failed to download artifact from {}: {}", pending.externalUrl(), e.getMessage());
      }
    }

    // Create artifact entity
    ArtifactEntity artifact = ArtifactEntity.builder()
        .projectId(projectId)
        .taskExecutionId(taskExecutionId)
        .name(pending.name())
        .kind(pending.kind())
        .description(pending.description())
        .prompt(pending.prompt())
        .model(pending.model())
        .storageKey(storageKey)
        .content(pending.inlineContent())
        .metadata(pending.metadata())
        .status(Status.CREATED)
        .deleted(false)
        .build();

    artifactRepository.save(artifact);
    log.info("Materialized artifact: {} ({})", pending.name(), artifact.getId());
  }

  // === Helper methods for checking artifact existence ===

  /**
   * Check if an artifact name exists in a TaskExecution's state.
   */
  public boolean artifactExistsAtTaskExecution(UUID taskExecutionId, String name) {
    List<ArtifactEntity> artifacts = getArtifactsAtTaskExecution(taskExecutionId);
    return artifacts.stream().anyMatch(a -> name.equals(a.getName()) && !a.isDeleted());
  }

  /**
   * Get the kind of an artifact at a TaskExecution.
   */
  public Optional<ArtifactEntity.Kind> getArtifactKindAtTaskExecution(UUID taskExecutionId, String name) {
    List<ArtifactEntity> artifacts = getArtifactsAtTaskExecution(taskExecutionId);
    return artifacts.stream()
        .filter(a -> name.equals(a.getName()) && !a.isDeleted())
        .map(ArtifactEntity::getKind)
        .findFirst();
  }
}
