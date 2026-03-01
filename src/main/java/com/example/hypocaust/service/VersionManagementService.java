package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.Changelist;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact version management operations. Handles artifact persistence and resolution.
 * Artifacts arrive already finalized (MANIFESTED or FAILED) from executors — no downloading
 * happens here.
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
      return new HashMap<>();
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
   * Gets the most recent committed snapshot of an artifact at a given task execution.
   */
  public Optional<Artifact> getMaterializedArtifactAt(@NonNull String name,
      UUID taskExecutionId) {
    Map<String, UUID> snapshot = computeArtifactSnapshotAt(taskExecutionId);
    UUID artifactId = snapshot.get(name);
    if (artifactId == null) {
      return Optional.empty();
    }
    return artifactService.getArtifact(artifactId);
  }

  public List<Artifact> getAllMaterializedArtifactsAt(UUID taskExecutionId) {
    Collection<UUID> artifactIds = computeArtifactSnapshotAt(taskExecutionId).values();
    return artifactService.getArtifacts(artifactIds);
  }

  public List<Artifact> getAllArtifactsWithChanges(UUID taskExecutionId,
      Changelist changelist) {
    return changelist.applyTo(getAllMaterializedArtifactsAt(taskExecutionId));
  }

  public Optional<Artifact> getArtifactWithChanges(String name, UUID taskExecutionId,
      Changelist changelist) {
    return getAllArtifactsWithChanges(taskExecutionId, changelist)
        .stream()
        .filter(a -> a.name().equals(name))
        .findFirst();
  }

  public boolean exists(String name, UUID taskExecutionId, Changelist changelist) {
    return getArtifactWithChanges(name, taskExecutionId, changelist).isPresent();
  }

  /**
   * Persist all pending artifacts to the database. Artifacts are already finalized — no downloading
   * or status transitions happen here.
   *
   * @param pending The accumulated changes containing artifacts to persist
   * @param taskExecutionId The TaskExecution these artifacts belong to
   * @param projectId The project these artifacts belong to
   * @return The delta of changes, or an empty delta if no changes
   */
  @Transactional
  public TaskExecutionDelta persist(Changelist pending, UUID taskExecutionId, UUID projectId) {
    if (!pending.hasChanges()) {
      log.info("No pending changes to persist");
      return new TaskExecutionDelta();
    }

    List<Artifact> persistedAdded = new ArrayList<>();
    for (Artifact artifact : pending.getAdded()) {
      persistedAdded.add(artifactService.persist(artifact, projectId, taskExecutionId));
    }

    List<Artifact> persistedEdited = new ArrayList<>();
    for (Artifact artifact : pending.getEdited()) {
      persistedEdited.add(artifactService.persist(artifact, projectId, taskExecutionId));
    }

    List<ArtifactChange> added = persistedAdded.stream()
        .map(a -> new ArtifactChange(a.name(), a.id()))
        .toList();

    List<ArtifactChange> edited = persistedEdited.stream()
        .map(a -> new ArtifactChange(a.name(), a.id()))
        .toList();

    List<String> deleted = new ArrayList<>(pending.getDeletedNames());

    TaskExecutionDelta delta = new TaskExecutionDelta(added, edited, deleted);
    log.info("Persisted {} artifacts (added: {}, edited: {}, deleted: {})",
        added.size() + edited.size(),
        delta.added().size(),
        delta.edited().size(),
        delta.deleted().size());

    return delta;
  }

  /**
   * Gets a historical artifact version by artifact name and execution name.
   */
  public Optional<Artifact> getMaterializedArtifactAtExecution(@NonNull String artifactName,
      @NonNull String executionName, @NonNull UUID projectId) {
    return taskExecutionRepository.findByProjectIdAndName(projectId, executionName)
        .flatMap(execution -> getMaterializedArtifactAt(artifactName, execution.getId()));
  }

  public Optional<UUID> getMostRecentTaskExecutionId(UUID projectId) {
    return taskExecutionRepository
        .findMostRecentCompletedByProjectId(projectId)
        .map(TaskExecutionEntity::getId);
  }

}
