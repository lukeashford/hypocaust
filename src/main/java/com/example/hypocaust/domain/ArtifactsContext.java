package com.example.hypocaust.domain;

import com.example.hypocaust.domain.event.ArtifactAddedEvent;
import com.example.hypocaust.domain.event.ArtifactRemovedEvent;
import com.example.hypocaust.domain.event.ArtifactUpdatedEvent;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.exception.ArtifactTypeMismatchException;
import com.example.hypocaust.service.VersionManagementService;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.utils.NamingUtils;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sub-context for managing artifacts during a TaskExecution.
 */
@Getter
@RequiredArgsConstructor
@Slf4j
public class ArtifactsContext {

  private final UUID projectId;
  private final UUID taskExecutionId;
  private final UUID predecessorId;

  private final EventService eventService;
  private final VersionManagementService versionService;

  private final Changelist changelist = new Changelist();

  private static final int GENERATED_NAME_MAX_LEN = 30;
  private static final int UPLOAD_NAME_MAX_LEN = 100;

  /**
   * Schedule a new artifact for creation. The preferred name is sanitized and deduplicated; the
   * preferred title is deduplicated by appending a counter if necessary.
   */
  public synchronized Artifact add(String preferredName, String preferredTitle, String description,
      ArtifactKind kind, JsonNode metadata) {
    String name = NamingUtils.sanitize(preferredName, GENERATED_NAME_MAX_LEN);
    name = NamingUtils.appendCounterIfExists(name, collectTakenNames());

    String title = NamingUtils.appendCounterIfExists(preferredTitle, collectTakenTitles());

    Artifact artifact = Artifact.builder()
        .name(name)
        .kind(kind)
        .title(title)
        .description(description)
        .status(ArtifactStatus.GESTATING)
        .metadata(metadata)
        .build();

    changelist.addArtifact(artifact);
    eventService.publish(new ArtifactAddedEvent(taskExecutionId, artifact));

    return artifact;
  }

  /**
   * Add an already-manifested artifact (e.g. a user upload that has been stored and analyzed). The
   * preferred name is sanitized and deduplicated; the preferred title is deduplicated.
   */
  public synchronized Artifact addManifested(String preferredName, String preferredTitle,
      String description, ArtifactKind kind, String storageKey, JsonNode inlineContent,
      String mimeType, JsonNode metadata) {
    String name = NamingUtils.sanitize(preferredName, UPLOAD_NAME_MAX_LEN);
    name = NamingUtils.appendCounterIfExists(name, collectTakenNames());

    String title = NamingUtils.appendCounterIfExists(preferredTitle, collectTakenTitles());

    Artifact artifact = Artifact.builder()
        .name(name)
        .kind(kind)
        .title(title)
        .description(description)
        .status(ArtifactStatus.MANIFESTED)
        .storageKey(storageKey)
        .inlineContent(inlineContent)
        .mimeType(mimeType)
        .metadata(metadata)
        .build();

    changelist.addArtifact(artifact);
    eventService.publish(new ArtifactAddedEvent(taskExecutionId, artifact));

    return artifact;
  }

  /**
   * Schedule an edit to an existing artifact.
   */
  public synchronized void edit(Artifact newVersion) {
    String name = newVersion.name();
    Artifact existing = versionService.getMaterializedArtifactAt(name, predecessorId)
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
    if (!versionService.exists(name, predecessorId, changelist)) {
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

  /**
   * Restore a historical artifact version into the current changelist.
   *
   * <p>Retrieves the artifact as it existed at the named task execution, then adds it as a new
   * artifact. The original name is reused if it is currently free; otherwise a new unique name is
   * generated automatically. The artifact retains its original status since storageKey persists
   * across executions.
   *
   * @param artifactName the artifact's semantic name in the historical snapshot
   * @param executionName the readable task execution name (e.g. "initial_character_designs")
   * @return the final name assigned to the restored artifact
   */
  public synchronized String restore(String artifactName, String executionName) {
    Artifact source = versionService.getMaterializedArtifactAtExecution(
            artifactName, executionName, projectId)
        .orElseThrow(() -> new ArtifactNotFoundException(
            "Artifact '" + artifactName + "' not found at execution '" + executionName + "'"));

    String name = NamingUtils.appendCounterIfExists(source.name(), collectTakenNames());
    String title = NamingUtils.appendCounterIfExists(source.title(), collectTakenTitles());
    String description = source.description();

    Artifact restored = Artifact.builder()
        .name(name)
        .kind(source.kind())
        .title(title)
        .description(description)
        .storageKey(source.storageKey())
        .inlineContent(source.inlineContent())
        .metadata(source.metadata())
        .mimeType(source.mimeType())
        .status(source.status())
        .build();

    changelist.addArtifact(restored);
    eventService.publish(new ArtifactAddedEvent(taskExecutionId, restored));

    log.info("Restored artifact '{}' from execution '{}' as '{}'",
        artifactName, executionName, name);

    return name;
  }

  private synchronized Set<String> collectTakenTitles() {
    return versionService.getAllArtifactsWithChanges(predecessorId, changelist)
        .stream()
        .map(Artifact::title)
        .collect(Collectors.toSet());
  }

  private synchronized Set<String> collectTakenNames() {
    return versionService.getAllArtifactsWithChanges(predecessorId, changelist)
        .stream()
        .map(Artifact::name)
        .collect(Collectors.toSet());
  }

  public synchronized Optional<Artifact> get(String name) {
    return versionService.getArtifactWithChanges(name, predecessorId, changelist);
  }

  public synchronized List<Artifact> getAllWithChanges() {
    return versionService.getAllArtifactsWithChanges(predecessorId, changelist);
  }
}
