package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.Changelist;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact version management operations. Handles artifact materialization and
 * resolution, but NOT task execution lifecycle. Task execution lifecycle (commit/fail) is managed
 * by TaskService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionManagementService {

  private final TaskExecutionRepository taskExecutionRepository;
  private final ArtifactService artifactService;

  /**
   * Recursively builds artifact state at a specific task execution.
   *
   * @param taskExecutionId the task execution id
   * @return a map of artifact names to artifact ids
   */
  public Map<String, UUID> computeArtifactSnapshotAt(UUID taskExecutionId) {
    if (taskExecutionId == null) { // root
      return Map.of();
    }

    TaskExecutionEntity execution = taskExecutionRepository.findById(taskExecutionId).orElseThrow(
        () -> new IllegalArgumentException("TaskExecution not found: " + taskExecutionId)
    );

    if (execution.getDelta() == null) {
      return computeArtifactSnapshotAt(execution.getPredecessorId());
    }

    Map<String, UUID> snapshot = computeArtifactSnapshotAt(execution.getPredecessorId());
    TaskExecutionDelta delta = execution.getDelta();
    if (!delta.hasChanges()) {
      return snapshot;
    }

    for (ArtifactChange change : delta.added()) {
      if (snapshot.containsKey(change.name())) {
        throw new IllegalStateException("Duplicate artifact name: " + change.name());
      }
      snapshot.put(change.name(), change.artifactId());
    }

    for (ArtifactChange change : delta.edited()) {
      if (!snapshot.containsKey(change.name())) {
        throw new IllegalStateException("Missing artifact name: " + change.name());
      }
      snapshot.put(change.name(), change.artifactId());
    }

    for (String name : delta.deleted()) {
      if (!snapshot.containsKey(name)) {
        throw new IllegalStateException("Missing artifact name: " + name);
      }
      snapshot.remove(name);
    }

    return snapshot;
  }

  /**
   * Gets the most recent committed snapshot of an artifact at a given task execution. DOES NOT
   * include ongoing changes.
   *
   * @param name the artifact name
   * @param taskExecutionId the task execution id (predecessor is used if still in progress)
   * @return the artifact, or empty if not found or deleted
   */
  public Optional<Artifact> getMaterializedArtifactAt(@NonNull String name,
      @NonNull UUID taskExecutionId) {
    Map<String, UUID> snapshot = computeArtifactSnapshotAt(taskExecutionId);
    UUID artifactId = snapshot.get(name);
    if (artifactId == null) {
      return Optional.empty();
    }
    return artifactService.getArtifact(artifactId);
  }

  public List<Artifact> getAllMaterializedArtifactsAt(@NonNull UUID taskExecutionId) {
    return computeArtifactSnapshotAt(taskExecutionId).values().stream()
        .map(artifactService::getArtifact)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  public List<Artifact> getAllArtifactsWithChanges(@NonNull UUID taskExecutionId,
      Changelist changelist) {
    return changelist.applyTo(getAllMaterializedArtifactsAt(taskExecutionId));
  }

  public Optional<Artifact> getArtifactWithChanges(String name, @NonNull UUID taskExecutionId,
      Changelist changelist) {
    return getAllArtifactsWithChanges(taskExecutionId, changelist)
        .stream()
        .filter(a -> a.name().equals(name))
        .findFirst();
  }

  public boolean exists(String name, @NonNull UUID taskExecutionId, Changelist changelist) {
    return getArtifactWithChanges(name, taskExecutionId, changelist).isPresent();
  }

  // === Artifact Materialization Operations ===

  @Transactional
  protected ArtifactChange materializeArtifact(Artifact pendingArtifact, UUID projectId,
      UUID taskExecutionId) {
    return new ArtifactChange(
        pendingArtifact.name(),
        artifactService.materialize(pendingArtifact, projectId, taskExecutionId)
    );
  }

  /**
   * Materialize all pending artifacts by downloading external inlineContent and storing in
   * database. Returns the delta representing what changed, or null if no changes.
   *
   * @param pending The accumulated changes containing artifacts to materialize
   * @param taskExecutionId The TaskExecution these artifacts belong to
   * @param projectId The project these artifacts belong to
   * @return The delta of changes, or null if no changes
   */
  @Transactional
  public TaskExecutionDelta materialize(Changelist pending, UUID taskExecutionId,
      UUID projectId) {
    if (!pending.hasChanges()) {
      log.info("No pending changes to materialize");
      return new TaskExecutionDelta();
    }

    List<ArtifactChange> added = new ArrayList<>();
    for (Artifact artifact : pending.getAdded()) {
      added.add(materializeArtifact(artifact, taskExecutionId, projectId));
    }

    List<ArtifactChange> edited = new ArrayList<>();
    for (Artifact artifact : pending.getEdited()) {
      edited.add(materializeArtifact(artifact, taskExecutionId, projectId));
    }

    List<String> deleted = new ArrayList<>(pending.getDeletedNames());

    TaskExecutionDelta delta = new TaskExecutionDelta(added, edited, deleted);
    log.info("Materialized {} artifacts (added: {}, edited: {}, deleted: {})",
        added.size() + edited.size(),
        delta.added().size(),
        delta.edited().size(),
        delta.deleted().size());

    return delta;
  }

  public Optional<UUID> getMostRecentTaskExecutionId(UUID projectId) {
    return taskExecutionRepository
        .findMostRecentCompletedByProjectId(projectId)
        .map(TaskExecutionEntity::getId);
  }

}
