package com.example.hypocaust.domain;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.domain.event.ArtifactRemovedEvent;
import com.example.hypocaust.domain.event.ArtifactUpdatedEvent;
import com.example.hypocaust.domain.event.TaskProgressUpdatedEvent;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.exception.ArtifactTypeMismatchException;
import com.example.hypocaust.service.ArtifactNameGeneratorService;
import com.example.hypocaust.service.ArtifactVersionManagementService;
import com.example.hypocaust.service.events.EventService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Thread-local context for the current TaskExecution. Replaces both RunContextHolder and
 * ExecutionContext with a unified approach. Also incorporates task progress tracking (previously in
 * TaskProgressService).
 */
@Getter
@RequiredArgsConstructor
public class TaskExecutionContext {

  private final UUID projectId;
  private final UUID taskExecutionId;
  private final UUID predecessorId;

  private final EventService eventService;
  private final ArtifactVersionManagementService versionService;
  private final ArtifactNameGeneratorService nameGeneratorService;

  private final PendingChanges pending = new PendingChanges();
  private final TaskTree taskProgress = new TaskTree();

  /**
   * Schedule a new artifact for creation. Generates a unique fileName from the description using a
   * small LLM. If the generated fileName conflicts, retries with an exclusion list until a unique
   * fileName is found. Emits artifact.added event with fileName, kind, description, externalUrl,
   * inlineContent, metadata.
   *
   * @return the generated artifact fileName (for use in subsequent updatePendingArtifact calls)
   */
  public String addArtifact(PendingArtifact artifact) {
    // Generate unique fileName from description
    String name = generateUniqueName(artifact.description());

    // Add to pending changes
    pending.addArtifact(name, artifact);

    // Emit event
    eventService.publish(new ArtifactAddedEvent(taskExecutionId, new ArtifactDto(
        name,
        artifact.kind(),
        artifact.externalUrl(),
        artifact.inlineContent(),
        artifact.title(),
        artifact.description(),
        true,
        artifact.status(),
        artifact.metadata()
    )));

    return name;
  }

  /**
   * Schedule an edit to an existing artifact (creates new version). Emits artifact.updated event
   * with fileName, kind, description, externalUrl, inlineContent, metadata.
   *
   * @throws ArtifactNotFoundException if fileName doesn't exist in predecessor's state
   * @throws ArtifactTypeMismatchException if pending artifact kind differs from existing
   */
  public void editArtifact(String name, PendingArtifact newVersion) {
    // Verify artifact exists
    if (!artifactExists(name)) {
      throw new ArtifactNotFoundException("Artifact not found: " + name);
    }

    // Verify type matches
    if (newVersion.kind() != null) {
      Optional<Kind> existingKind = predecessorId != null
          ? versionService.getArtifactKindAtTaskExecution(predecessorId, name)
          : Optional.empty();
      if (existingKind.isPresent() && !existingKind.get().equals(newVersion.kind())) {
        throw new ArtifactTypeMismatchException(name, existingKind.get(), newVersion.kind());
      }
    }

    // Add to pending changes
    pending.editArtifact(name, newVersion);

    // Emit event
    eventService.publish(new ArtifactUpdatedEvent(taskExecutionId, new ArtifactDto(
        name,
        newVersion.kind(),
        newVersion.externalUrl(),
        newVersion.inlineContent(),
        newVersion.title(),
        newVersion.description(),
        true,
        newVersion.status(),
        newVersion.metadata()
    )));
  }

  /**
   * Schedule an artifact for deletion (soft delete). Emits artifact.removed event with fileName.
   *
   * @throws ArtifactNotFoundException if fileName doesn't exist in predecessor's state
   */
  public void deleteArtifact(String name) {
    // Verify artifact exists
    if (!artifactExists(name)) {
      throw new ArtifactNotFoundException("Artifact not found: " + name);
    }

    // Mark for deletion
    pending.deleteArtifact(name);

    // Emit event
    eventService.publish(new ArtifactRemovedEvent(taskExecutionId, name));
  }

  /**
   * Update a pending artifact that was previously scheduled via addArtifact or editArtifact. Does
   * NOT introduce a new change - just updates the pending entry. Emits artifact.updated event with
   * fileName, kind, description, externalUrl, inlineContent, metadata. Used by operators to update
   * progress (e.g., streaming text tokens, image generation completion).
   *
   * @throws IllegalStateException if no pending artifact with this fileName exists
   */
  public void updatePendingArtifact(String name, PendingArtifact newVersion) {
    // Use atomic method to avoid race condition
    boolean updated = pending.updateIfPending(name, newVersion);
    if (!updated) {
      throw new IllegalStateException("No pending artifact with fileName: " + name);
    }

    // Emit event
    eventService.publish(new ArtifactUpdatedEvent(taskExecutionId, new ArtifactDto(
        name,
        newVersion.kind(),
        newVersion.externalUrl(),
        newVersion.inlineContent(),
        newVersion.title(),
        newVersion.description(),
        true,
        newVersion.status(),
        newVersion.metadata()
    )));
  }

  /**
   * Cancel a pending artifact that was previously scheduled. Emits artifact.removed event with
   * fileName.
   *
   * @throws IllegalStateException if no pending artifact with this fileName exists
   */
  public void cancelPendingArtifact(String name) {
    // Use atomic method to avoid race condition
    boolean cancelled = pending.cancelIfPending(name);
    if (!cancelled) {
      throw new IllegalStateException("No pending artifact with fileName: " + name);
    }

    // Emit event
    eventService.publish(new ArtifactRemovedEvent(taskExecutionId, name));
  }

  /**
   * Check if an artifact fileName exists in current state.
   */
  public boolean artifactExists(String name) {
    // Check pending first
    if (pending.isPending(name) && !pending.isDeleted(name)) {
      return true;
    }

    // Check predecessor state
    return predecessorId != null
        && versionService.artifactExistsAtTaskExecution(predecessorId, name)
        && !pending.isDeleted(name);
  }

  /**
   * Get current artifact state (predecessor, adjusted by pending).
   */
  public List<ArtifactDto> getCurrentArtifacts() {
    List<ArtifactDto> result = new ArrayList<>();

    // Get artifacts from predecessor
    if (predecessorId != null) {
      List<ArtifactEntity> predecessorArtifacts = versionService.getArtifactsAtTaskExecution(
          predecessorId);
      for (ArtifactEntity entity : predecessorArtifacts) {
        // Skip deleted artifacts
        if (pending.isDeleted(entity.getFileName())) {
          continue;
        }

        // Check if there's a pending edit
        Optional<PendingArtifact> pendingEdit = pending.getPendingArtifact(entity.getFileName());
        if (pendingEdit.isPresent() && !pending.isAdded(entity.getFileName())) {
          // Return the pending version
          PendingArtifact pa = pendingEdit.get();
          result.add(new ArtifactDto(
              pa.name(),
              pa.kind(),
              pa.externalUrl(),
              pa.inlineContent(),
              pa.title(),
              pa.description(),
              true,
              pa.status(),
              pa.metadata()
          ));
        } else {
          // Return the existing version
          result.add(new ArtifactDto(
              entity.getFileName(),
              entity.getKind(),
              entity.getStorageKey() != null ? "/artifacts/" + entity.getId() + "/content" : null,
              entity.getContent(),
              entity.getTitle(),
              entity.getDescription(),
              false,
              entity.getStatus(),
              entity.getMetadata()
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
            pa.externalUrl(),
            pa.inlineContent(),
            pa.title(),
            pa.description(),
            true,
            pa.status(),
            pa.metadata()
        ));
      }
    }

    return result;
  }

  // === Task Progress (thread-local) ===

  /**
   * Publish subtasks for a path. Called by operators to declare their planned work.
   */
  public void publishSubtasks(String pathPrefix, List<TaskItem> subtasks) {
    taskProgress.addSubtasks(pathPrefix, subtasks);
    eventService.publish(new TaskProgressUpdatedEvent(taskExecutionId, taskProgress));
  }

  /**
   * Update a task's status.
   */
  public void updateTaskStatus(String taskId, TaskStatus status) {
    taskProgress.updateStatus(taskId, status);
    eventService.publish(new TaskProgressUpdatedEvent(taskExecutionId, taskProgress));
  }

  /**
   * Get the full task tree for this TaskExecution.
   */
  public TaskTree getTaskTree() {
    return taskProgress;
  }

  // === Helper methods ===

  private String generateUniqueName(String description) {
    Set<String> existingNames = predecessorId != null
        ? versionService.getArtifactsAtTaskExecution(predecessorId).stream()
        .filter(a -> a.getStatus() != Status.DELETED)
        .map(ArtifactEntity::getFileName)
        .collect(Collectors.toSet())
        : new java.util.HashSet<>();

    // Add pending names to exclusion list
    existingNames.addAll(pending.getAddedNames());
    existingNames.addAll(pending.getEditedNames());

    return nameGeneratorService.generateUniqueName(description, existingNames);
  }
}
