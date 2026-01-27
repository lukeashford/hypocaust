package com.example.hypocaust.domain;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.db.ArtifactEntity.Status;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Accumulator for changes made during a TaskExecution.
 * Uses name-based tracking for artifacts.
 * Thread-safe for concurrent modifications.
 */
public class PendingChanges {

  private final Map<String, PendingArtifact> added = new LinkedHashMap<>();
  private final Map<String, PendingArtifact> edited = new LinkedHashMap<>();
  private final Set<String> deleted = new LinkedHashSet<>();

  /**
   * Add a new artifact.
   */
  public synchronized void addArtifact(String name, PendingArtifact artifact) {
    added.put(name, artifact.withName(name).withStatus(Status.SCHEDULED));
  }

  /**
   * Edit an existing artifact (creates new version).
   */
  public synchronized void editArtifact(String name, PendingArtifact artifact) {
    edited.put(name, artifact.withName(name).withStatus(Status.SCHEDULED));
  }

  /**
   * Update a pending artifact (either added or edited).
   */
  public synchronized void updatePendingArtifact(String name, PendingArtifact artifact) {
    if (added.containsKey(name)) {
      added.put(name, artifact.withName(name));
    } else if (edited.containsKey(name)) {
      edited.put(name, artifact.withName(name));
    }
  }

  /**
   * Mark an artifact for deletion.
   */
  public synchronized void deleteArtifact(String name) {
    deleted.add(name);
  }

  /**
   * Cancel a pending artifact.
   */
  public synchronized void cancelPendingArtifact(String name) {
    if (added.containsKey(name)) {
      PendingArtifact artifact = added.get(name);
      added.put(name, artifact.withStatus(Status.CANCELLED));
    } else if (edited.containsKey(name)) {
      PendingArtifact artifact = edited.get(name);
      edited.put(name, artifact.withStatus(Status.CANCELLED));
    }
  }

  /**
   * Check if an artifact is pending (added or edited).
   */
  public synchronized boolean isPending(String name) {
    return added.containsKey(name) || edited.containsKey(name);
  }

  /**
   * Check if an artifact is being added (not edited).
   */
  public synchronized boolean isAdded(String name) {
    return added.containsKey(name);
  }

  /**
   * Check if an artifact is marked for deletion.
   */
  public synchronized boolean isDeleted(String name) {
    return deleted.contains(name);
  }

  /**
   * Get a pending artifact by name.
   */
  public synchronized Optional<PendingArtifact> getPendingArtifact(String name) {
    if (added.containsKey(name)) {
      return Optional.of(added.get(name));
    }
    if (edited.containsKey(name)) {
      return Optional.of(edited.get(name));
    }
    return Optional.empty();
  }

  /**
   * Get all added artifact names.
   */
  public synchronized List<String> getAddedNames() {
    return new ArrayList<>(added.keySet());
  }

  /**
   * Get all edited artifact names.
   */
  public synchronized List<String> getEditedNames() {
    return new ArrayList<>(edited.keySet());
  }

  /**
   * Get all deleted artifact names.
   */
  public synchronized List<String> getDeletedNames() {
    return new ArrayList<>(deleted);
  }

  /**
   * Get all added artifacts.
   */
  public synchronized List<PendingArtifact> getAdded() {
    return new ArrayList<>(added.values());
  }

  /**
   * Get all edited artifacts.
   */
  public synchronized List<PendingArtifact> getEdited() {
    return new ArrayList<>(edited.values());
  }

  /**
   * Get all pending artifacts (added and edited) that are not cancelled.
   */
  public synchronized List<PendingArtifact> getActivePending() {
    List<PendingArtifact> active = new ArrayList<>();
    for (PendingArtifact artifact : added.values()) {
      if (artifact.status() != Status.CANCELLED) {
        active.add(artifact);
      }
    }
    for (PendingArtifact artifact : edited.values()) {
      if (artifact.status() != Status.CANCELLED) {
        active.add(artifact);
      }
    }
    return active;
  }

  /**
   * Get all cancelled pending artifacts.
   */
  public synchronized List<PendingArtifact> getCancelled() {
    List<PendingArtifact> cancelled = new ArrayList<>();
    for (PendingArtifact artifact : added.values()) {
      if (artifact.status() == Status.CANCELLED) {
        cancelled.add(artifact);
      }
    }
    for (PendingArtifact artifact : edited.values()) {
      if (artifact.status() == Status.CANCELLED) {
        cancelled.add(artifact);
      }
    }
    return cancelled;
  }

  /**
   * Check if there are any changes.
   */
  public synchronized boolean hasChanges() {
    // Filter out cancelled artifacts when checking for changes
    boolean hasActiveAdded = added.values().stream()
        .anyMatch(a -> a.status() != Status.CANCELLED);
    boolean hasActiveEdited = edited.values().stream()
        .anyMatch(a -> a.status() != Status.CANCELLED);
    return hasActiveAdded || hasActiveEdited || !deleted.isEmpty();
  }

  /**
   * Build a TaskExecutionDelta from these pending changes.
   * Only includes non-cancelled artifacts.
   */
  public synchronized TaskExecutionDelta toTaskExecutionDelta() {
    List<ArtifactChange> addedChanges = added.entrySet().stream()
        .filter(e -> e.getValue().status() != Status.CANCELLED)
        .map(e -> new ArtifactChange(e.getKey()))
        .toList();

    List<ArtifactChange> editedChanges = edited.entrySet().stream()
        .filter(e -> e.getValue().status() != Status.CANCELLED)
        .map(e -> new ArtifactChange(e.getKey()))
        .toList();

    return new TaskExecutionDelta(addedChanges, editedChanges, new ArrayList<>(deleted));
  }

  /**
   * Clear all pending changes.
   */
  public synchronized void clear() {
    added.clear();
    edited.clear();
    deleted.clear();
  }

  /**
   * Get the kind of a pending artifact.
   */
  public synchronized Optional<Kind> getPendingKind(String name) {
    if (added.containsKey(name)) {
      return Optional.ofNullable(added.get(name).kind());
    }
    if (edited.containsKey(name)) {
      return Optional.ofNullable(edited.get(name).kind());
    }
    return Optional.empty();
  }
}
