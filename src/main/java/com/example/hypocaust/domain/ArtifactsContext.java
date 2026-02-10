package com.example.hypocaust.domain;

import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.domain.event.ArtifactRemovedEvent;
import com.example.hypocaust.domain.event.ArtifactUpdatedEvent;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.exception.ArtifactTypeMismatchException;
import com.example.hypocaust.service.ArtifactNameGeneratorService;
import com.example.hypocaust.service.VersionManagementService;
import com.example.hypocaust.service.events.EventService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sub-context for managing artifacts during a TaskExecution.
 */
@Getter
@RequiredArgsConstructor
public class ArtifactsContext {

  private final UUID projectId;
  private final UUID taskExecutionId;
  private final UUID predecessorId;

  private final EventService eventService;
  private final VersionManagementService versionService;
  private final ArtifactNameGeneratorService nameGeneratorService;

  private final Changelist changelist = new Changelist();

  /**
   * Schedule a new artifact for creation.
   */
  public synchronized String add(ArtifactDraft draft) {
    String name = generateUniqueName(draft.description());
    Artifact artifact = Artifact.fromDraft(name, draft);
    changelist.addArtifact(artifact);

    eventService.publish(new ArtifactAddedEvent(taskExecutionId, artifact));

    return name;
  }

  /**
   * Schedule an edit to an existing artifact.
   */
  public synchronized void edit(Artifact newVersion) {
    String name = newVersion.name();
    Artifact existing = versionService.getMaterializedArtifactAt(name, taskExecutionId)
        .orElseThrow(
            () -> new ArtifactNotFoundException("Artifact not found: " + name));

    if (!existing.kind().equals(newVersion.kind())) {
      throw new ArtifactTypeMismatchException(name, existing.kind(),
          newVersion.kind());
    }

    changelist.editArtifact(newVersion);

    eventService.publish(new ArtifactUpdatedEvent(taskExecutionId, newVersion));
  }

  /**
   * Schedule an artifact for deletion.
   */
  public synchronized void delete(String name) {
    if (!versionService.exists(name, taskExecutionId, changelist)) {
      throw new ArtifactNotFoundException("Artifact not found: " + name);
    }

    changelist.deleteArtifact(name);
    eventService.publish(new ArtifactRemovedEvent(taskExecutionId, name));
  }

  /**
   * Update a changelist artifact.
   */
  public synchronized void updatePending(Artifact newVersion) {
    if (!changelist.contains(newVersion.name())) {
      throw new IllegalStateException("No changelist artifact with name: " + newVersion.name());
    }
    changelist.updateArtifact(newVersion);

    eventService.publish(new ArtifactUpdatedEvent(taskExecutionId, newVersion));
  }

  /**
   * Rollback a changelist artifact (removes from changelist entirely).
   */
  public synchronized void rollbackPending(String name) {
    if (!changelist.contains(name)) {
      throw new IllegalStateException("No changelist artifact with name: " + name);
    }
    changelist.rollbackArtifact(name);

    eventService.publish(new ArtifactRemovedEvent(taskExecutionId, name));
  }

  private synchronized String generateUniqueName(String description) {
    Set<String> existingNames = versionService.computeArtifactSnapshotAt(taskExecutionId).keySet();

    existingNames.addAll(changelist.getAddedNames());
    existingNames.addAll(changelist.getEditedNames());
    changelist.getDeletedNames().forEach(existingNames::remove);

    return nameGeneratorService.generateUniqueName(description, existingNames);
  }

  public synchronized Optional<Artifact> get(String name) {
    return versionService.getArtifactWithChanges(name, taskExecutionId, changelist);
  }

  public synchronized List<Artifact> getAllWithChanges() {
    return versionService.getAllArtifactsWithChanges(taskExecutionId, changelist);
  }

  public synchronized boolean exists(String name) {
    return versionService.exists(name, taskExecutionId, changelist);
  }
}
