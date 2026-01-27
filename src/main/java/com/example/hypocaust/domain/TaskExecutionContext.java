package com.example.hypocaust.domain;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.exception.ArtifactTypeMismatchException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.Getter;

/**
 * Thread-local context for the current TaskExecution.
 * Replaces both RunContextHolder and ExecutionContext with a unified approach.
 * Also incorporates task progress tracking (previously in TaskProgressService).
 */
@Getter
public class TaskExecutionContext {

  private final UUID projectId;
  private final UUID taskExecutionId;
  private final UUID predecessorId;
  private final PendingChanges pending;
  private final TaskTree taskProgress;

  // Callbacks for event emission
  private Consumer<ArtifactAddedEventData> onArtifactAdded;
  private Consumer<ArtifactUpdatedEventData> onArtifactUpdated;
  private Consumer<ArtifactRemovedEventData> onArtifactRemoved;
  private Consumer<TaskTree> onTaskProgressUpdated;

  // Function to check if artifact exists in predecessor state
  private Function<String, Boolean> artifactExistsChecker;

  // Function to get artifact kind from predecessor state
  private Function<String, Optional<Kind>> artifactKindGetter;

  // Function to get all artifact names from predecessor state
  private Function<Void, Set<String>> artifactNamesGetter;

  // Function to generate artifact name from description
  private Function<NameGenerationRequest, String> nameGenerator;

  // Function to get current artifacts (for getCurrentArtifacts method)
  private Function<Void, List<ArtifactEntity>> currentArtifactsGetter;

  public TaskExecutionContext(UUID projectId, UUID taskExecutionId, UUID predecessorId) {
    this.projectId = projectId;
    this.taskExecutionId = taskExecutionId;
    this.predecessorId = predecessorId;
    this.pending = new PendingChanges();
    this.taskProgress = new TaskTree();
  }

  // === Event callback setters ===

  public void setOnArtifactAdded(Consumer<ArtifactAddedEventData> callback) {
    this.onArtifactAdded = callback;
  }

  public void setOnArtifactUpdated(Consumer<ArtifactUpdatedEventData> callback) {
    this.onArtifactUpdated = callback;
  }

  public void setOnArtifactRemoved(Consumer<ArtifactRemovedEventData> callback) {
    this.onArtifactRemoved = callback;
  }

  public void setOnTaskProgressUpdated(Consumer<TaskTree> callback) {
    this.onTaskProgressUpdated = callback;
  }

  public void setArtifactExistsChecker(Function<String, Boolean> checker) {
    this.artifactExistsChecker = checker;
  }

  public void setArtifactKindGetter(Function<String, Optional<Kind>> getter) {
    this.artifactKindGetter = getter;
  }

  public void setArtifactNamesGetter(Function<Void, Set<String>> getter) {
    this.artifactNamesGetter = getter;
  }

  public void setNameGenerator(Function<NameGenerationRequest, String> generator) {
    this.nameGenerator = generator;
  }

  public void setCurrentArtifactsGetter(Function<Void, List<ArtifactEntity>> getter) {
    this.currentArtifactsGetter = getter;
  }

  // === Artifact Hooks (called by operators) ===

  /**
   * Schedule a new artifact for creation.
   * Generates a unique name from the description using a small LLM.
   * If the generated name conflicts, retries with an exclusion list until a unique name is found.
   * Emits artifact.added event with name, kind, description, externalUrl, inlineContent, metadata.
   *
   * @return the generated artifact name (for use in subsequent updatePendingArtifact calls)
   */
  public String addArtifact(PendingArtifact artifact) {
    // Generate unique name from description
    String name = generateUniqueName(artifact.description());

    // Add to pending changes
    pending.addArtifact(name, artifact);

    // Emit event
    if (onArtifactAdded != null) {
      onArtifactAdded.accept(new ArtifactAddedEventData(
          name,
          artifact.kind(),
          artifact.description(),
          artifact.externalUrl(),
          artifact.inlineContent(),
          artifact.metadata()
      ));
    }

    return name;
  }

  /**
   * Schedule an edit to an existing artifact (creates new version).
   * Emits artifact.updated event with name, kind, description, externalUrl, inlineContent, metadata.
   *
   * @throws ArtifactNotFoundException     if name doesn't exist in predecessor's state
   * @throws ArtifactTypeMismatchException if pending artifact kind differs from existing
   */
  public void editArtifact(String name, PendingArtifact newVersion) {
    // Verify artifact exists
    if (!artifactExists(name)) {
      throw new ArtifactNotFoundException("Artifact not found: " + name);
    }

    // Verify type matches if kind getter is available
    if (artifactKindGetter != null && newVersion.kind() != null) {
      Optional<Kind> existingKind = artifactKindGetter.apply(name);
      if (existingKind.isPresent() && !existingKind.get().equals(newVersion.kind())) {
        throw new ArtifactTypeMismatchException(name, existingKind.get(), newVersion.kind());
      }
    }

    // Add to pending changes
    pending.editArtifact(name, newVersion);

    // Emit event
    if (onArtifactUpdated != null) {
      onArtifactUpdated.accept(new ArtifactUpdatedEventData(
          name,
          newVersion.description(),
          newVersion.externalUrl(),
          newVersion.inlineContent(),
          newVersion.metadata()
      ));
    }
  }

  /**
   * Schedule an artifact for deletion (soft delete).
   * Emits artifact.removed event with name.
   *
   * @throws ArtifactNotFoundException if name doesn't exist in predecessor's state
   */
  public void deleteArtifact(String name) {
    // Verify artifact exists
    if (!artifactExists(name)) {
      throw new ArtifactNotFoundException("Artifact not found: " + name);
    }

    // Mark for deletion
    pending.deleteArtifact(name);

    // Emit event
    if (onArtifactRemoved != null) {
      onArtifactRemoved.accept(new ArtifactRemovedEventData(name));
    }
  }

  /**
   * Update a pending artifact that was previously scheduled via addArtifact or editArtifact.
   * Does NOT introduce a new change - just updates the pending entry.
   * Emits artifact.updated event with name, kind, description, externalUrl, inlineContent, metadata.
   * Used by operators to update progress (e.g., streaming text tokens, image generation completion).
   *
   * @throws IllegalStateException if no pending artifact with this name exists
   */
  public void updatePendingArtifact(String name, PendingArtifact newVersion) {
    if (!pending.isPending(name)) {
      throw new IllegalStateException("No pending artifact with name: " + name);
    }

    // Update pending
    pending.updatePendingArtifact(name, newVersion);

    // Emit event
    if (onArtifactUpdated != null) {
      onArtifactUpdated.accept(new ArtifactUpdatedEventData(
          name,
          newVersion.description(),
          newVersion.externalUrl(),
          newVersion.inlineContent(),
          newVersion.metadata()
      ));
    }
  }

  /**
   * Cancel a pending artifact that was previously scheduled.
   * Emits artifact.removed event with name.
   *
   * @throws IllegalStateException if no pending artifact with this name exists
   */
  public void cancelPendingArtifact(String name) {
    if (!pending.isPending(name)) {
      throw new IllegalStateException("No pending artifact with name: " + name);
    }

    // Cancel pending
    pending.cancelPendingArtifact(name);

    // Emit event
    if (onArtifactRemoved != null) {
      onArtifactRemoved.accept(new ArtifactRemovedEventData(name));
    }
  }

  /**
   * Check if an artifact name exists in current state.
   */
  public boolean artifactExists(String name) {
    // Check pending first
    if (pending.isPending(name) && !pending.isDeleted(name)) {
      return true;
    }

    // Check predecessor state
    if (artifactExistsChecker != null) {
      return artifactExistsChecker.apply(name) && !pending.isDeleted(name);
    }

    return false;
  }

  /**
   * Get current artifact state (predecessor, adjusted by pending).
   */
  public List<ArtifactDto> getCurrentArtifacts() {
    List<ArtifactDto> result = new ArrayList<>();

    // Get artifacts from predecessor
    if (currentArtifactsGetter != null) {
      List<ArtifactEntity> predecessorArtifacts = currentArtifactsGetter.apply(null);
      for (ArtifactEntity entity : predecessorArtifacts) {
        // Skip deleted artifacts
        if (pending.isDeleted(entity.getName())) {
          continue;
        }

        // Check if there's a pending edit
        Optional<PendingArtifact> pendingEdit = pending.getPendingArtifact(entity.getName());
        if (pendingEdit.isPresent() && !pending.isAdded(entity.getName())) {
          // Return the pending version
          PendingArtifact pa = pendingEdit.get();
          result.add(new ArtifactDto(
              pa.name(),
              pa.kind(),
              pa.description(),
              pa.externalUrl(),
              true,
              pa.status()
          ));
        } else {
          // Return the existing version
          result.add(new ArtifactDto(
              entity.getName(),
              entity.getKind(),
              entity.getDescription(),
              entity.getStorageKey() != null ? "/artifacts/" + entity.getId() + "/content" : null,
              false,
              entity.getStatus()
          ));
        }
      }
    }

    // Add pending new artifacts
    for (PendingArtifact pa : pending.getAdded()) {
      if (pa.status() != Status.CANCELLED) {
        result.add(new ArtifactDto(
            pa.name(),
            pa.kind(),
            pa.description(),
            pa.externalUrl(),
            true,
            pa.status()
        ));
      }
    }

    return result;
  }

  // === Task Progress (thread-local) ===

  /**
   * Publish subtasks for a path.
   * Called by operators to declare their planned work.
   */
  public void publishSubtasks(String pathPrefix, List<TaskItem> subtasks) {
    taskProgress.addSubtasks(pathPrefix, subtasks);

    if (onTaskProgressUpdated != null) {
      onTaskProgressUpdated.accept(taskProgress);
    }
  }

  /**
   * Update a task's status.
   */
  public void updateTaskStatus(String taskId, TaskStatus status) {
    taskProgress.updateStatus(taskId, status);

    if (onTaskProgressUpdated != null) {
      onTaskProgressUpdated.accept(taskProgress);
    }
  }

  /**
   * Get the full task tree for this TaskExecution.
   */
  public TaskTree getTaskTree() {
    return taskProgress;
  }

  // === Helper methods ===

  private String generateUniqueName(String description) {
    if (nameGenerator == null) {
      // Fallback: generate simple name from description
      return sanitizeName(description);
    }

    Set<String> existingNames = artifactNamesGetter != null
        ? artifactNamesGetter.apply(null)
        : Set.of();

    // Add pending names to exclusion list
    existingNames = new java.util.HashSet<>(existingNames);
    existingNames.addAll(pending.getAddedNames());
    existingNames.addAll(pending.getEditedNames());

    return nameGenerator.apply(new NameGenerationRequest(description, existingNames));
  }

  private String sanitizeName(String description) {
    // Simple fallback: convert description to snake_case
    return description.toLowerCase()
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_+|_+$", "")
        .substring(0, Math.min(50, description.length()));
  }

  // === Event data records ===

  public record ArtifactAddedEventData(
      String name,
      Kind kind,
      String description,
      String externalUrl,
      com.fasterxml.jackson.databind.JsonNode inlineContent,
      com.fasterxml.jackson.databind.JsonNode metadata
  ) {}

  public record ArtifactUpdatedEventData(
      String name,
      String description,
      String externalUrl,
      com.fasterxml.jackson.databind.JsonNode inlineContent,
      com.fasterxml.jackson.databind.JsonNode metadata
  ) {}

  public record ArtifactRemovedEventData(String name) {}

  public record NameGenerationRequest(String description, Set<String> existingNames) {}
}
