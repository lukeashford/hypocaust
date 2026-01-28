package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.PendingArtifact;
import com.example.hypocaust.domain.PendingChanges;
import com.example.hypocaust.domain.TaskExecutionDelta;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact version management operations.
 * Handles artifact materialization and resolution, but NOT task execution lifecycle.
 * Task execution lifecycle (commit/fail) is managed by TaskService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactVersionManagementService {

  private final TaskExecutionRepository taskExecutionRepository;
  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;

  // === Artifact Materialization Operations ===

  /**
   * Materialize all pending artifacts by downloading external content and storing in database.
   * Returns the delta representing what changed, or null if no changes.
   *
   * @param pending         The accumulated changes containing artifacts to materialize
   * @param taskExecutionId The TaskExecution these artifacts belong to
   * @param projectId       The project these artifacts belong to
   * @return The delta of changes, or null if no changes
   */
  @Transactional
  public TaskExecutionDelta materialize(PendingChanges pending, UUID taskExecutionId, UUID projectId) {
    if (!pending.hasChanges()) {
      log.info("No pending changes to materialize");
      return null;
    }

    for (PendingArtifact artifact : pending.getActivePending()) {
      materializeArtifact(artifact, taskExecutionId, projectId);
    }

    TaskExecutionDelta delta = pending.toTaskExecutionDelta();
    log.info("Materialized {} artifacts (added: {}, edited: {}, deleted: {})",
        pending.getActivePending().size(),
        delta.added().size(),
        delta.edited().size(),
        delta.deleted().size());

    return delta;
  }

  /**
   * Discard all pending changes without persisting.
   * Called when a task execution fails.
   *
   * @param pending The pending changes to discard
   */
  public void discardPending(PendingChanges pending) {
    if (pending.hasChanges()) {
      log.info("Discarding {} pending artifacts due to task failure",
          pending.getActivePending().size());
    }
    // No actual work needed - the pending changes are simply not persisted
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
