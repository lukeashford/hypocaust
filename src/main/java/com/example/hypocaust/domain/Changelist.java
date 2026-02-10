package com.example.hypocaust.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Accumulator for changes made during a TaskExecution. Uses name-based tracking for artifacts.
 * Thread-safe for concurrent modifications.
 */
@Slf4j
public class Changelist {

  private final Map<String, Artifact> added = new LinkedHashMap<>();
  private final Map<String, Artifact> edited = new LinkedHashMap<>();
  private final Set<String> deleted = new LinkedHashSet<>();

  /**
   * Add a new artifact with an explicit name.
   */
  public synchronized void addArtifact(Artifact artifact) {
    String name = artifact.name();
    if (added.containsKey(name) || edited.containsKey(name)) {
      throw new IllegalArgumentException("Artifact with name already exists: " + name);
    }

    if (deleted.remove(name)) {
      edited.put(name, artifact);
    } else {
      added.put(name, artifact);
    }
  }

  /**
   * Edit an existing artifact (creates new version).
   */
  public synchronized void editArtifact(Artifact artifact) {
    String name = artifact.name();
    deleted.remove(name);
    edited.put(name, artifact);
  }

  /**
   * Mark an artifact for deletion.
   */
  public synchronized void deleteArtifact(String name) {
    if (added.containsKey(name)) {
      added.remove(name);
    } else {
      edited.remove(name);
      deleted.add(name);
    }
  }

  /**
   * Update a pending artifact (either added or edited).
   */
  public synchronized void updateArtifact(Artifact artifact) {
    String name = artifact.name();
    if (added.containsKey(name)) {
      added.put(name, artifact);
    } else if (edited.containsKey(name) || deleted.contains(name)) {
      deleted.remove(name);
      edited.put(name, artifact);
    } else {
      throw new IllegalStateException("No pending artifact with name: " + name);
    }
  }

  /**
   * Rollback a pending artifact (removes from changelist entirely).
   */
  public synchronized void rollbackArtifact(String name) {
    if (!contains(name)) {
      log.warn("No pending artifact with name: {}", name);
      return;
    }
    added.remove(name);
    edited.remove(name);
    deleted.remove(name);
  }

  public synchronized List<Artifact> applyTo(List<Artifact> artifacts) {
    List<Artifact> result = new ArrayList<>(artifacts);
    for (Artifact artifact : added.values()) {
      if (result.stream().anyMatch(a -> a.name().equals(artifact.name()))) {
        throw new IllegalStateException("Duplicate artifact name: " + artifact.name());
      }
      result.add(artifact);
    }
    for (Artifact artifact : edited.values()) {
      if (result.stream().noneMatch(a -> a.name().equals(artifact.name()))) {
        throw new IllegalStateException("No artifact with name: " + artifact.name());
      }
      result.removeIf(a -> a.name().equals(artifact.name()));
      result.add(artifact);
    }
    for (String name : deleted) {
      if (result.stream().noneMatch(a -> a.name().equals(name))) {
        throw new IllegalStateException("No artifact with name: " + name);
      }
      result.removeIf(a -> a.name().equals(name));
    }

    return result;
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
  public synchronized List<Artifact> getAdded() {
    return new ArrayList<>(added.values());
  }

  /**
   * Get all edited artifacts.
   */
  public synchronized List<Artifact> getEdited() {
    return new ArrayList<>(edited.values());
  }

  /**
   * Check if the changelist contains an artifact with the given name.
   */
  public synchronized boolean contains(String name) {
    return added.containsKey(name) || edited.containsKey(name) || deleted.contains(name);
  }

  /**
   * Check if there are any changes.
   */
  public synchronized boolean hasChanges() {
    return !added.isEmpty() || !edited.isEmpty() || !deleted.isEmpty();
  }
}
