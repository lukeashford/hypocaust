package com.example.hypocaust.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Accumulator for changes made during a run execution.
 * Changes are accumulated here and then atomically committed at the end.
 *
 * <p>This class is mutable and thread-safe for use during execution.
 */
public class PendingChanges {

  private final Set<UUID> regenerating;
  private final Set<UUID> keeping;
  private final List<ArtifactNode> created;
  private final List<ArtifactRelation> relations;

  public PendingChanges() {
    this.regenerating = new HashSet<>();
    this.keeping = new HashSet<>();
    this.created = new ArrayList<>();
    this.relations = new ArrayList<>();
  }

  /**
   * Mark an artifact for regeneration (will be superseded).
   */
  public synchronized void markForRegeneration(UUID artifactId) {
    keeping.remove(artifactId);
    regenerating.add(artifactId);
  }

  /**
   * Explicitly keep an artifact (won't be touched).
   */
  public synchronized void keep(UUID artifactId) {
    regenerating.remove(artifactId);
    keeping.add(artifactId);
  }

  /**
   * Add a newly created artifact.
   */
  public synchronized void addCreated(ArtifactNode artifact) {
    created.add(artifact);
  }

  /**
   * Add a relation between artifacts.
   */
  public synchronized void addRelation(ArtifactRelation relation) {
    relations.add(relation);
  }

  /**
   * Check if an artifact is marked for regeneration.
   */
  public synchronized boolean isMarkedForRegeneration(UUID artifactId) {
    return regenerating.contains(artifactId);
  }

  /**
   * Check if an artifact is explicitly kept.
   */
  public synchronized boolean isKept(UUID artifactId) {
    return keeping.contains(artifactId);
  }

  /**
   * Get all artifact IDs marked for regeneration.
   */
  public synchronized Set<UUID> getRegenerating() {
    return Set.copyOf(regenerating);
  }

  /**
   * Get all artifact IDs explicitly kept.
   */
  public synchronized Set<UUID> getKeeping() {
    return Set.copyOf(keeping);
  }

  /**
   * Get all newly created artifacts.
   */
  public synchronized List<ArtifactNode> getCreated() {
    return List.copyOf(created);
  }

  /**
   * Get all added relations.
   */
  public synchronized List<ArtifactRelation> getRelations() {
    return List.copyOf(relations);
  }

  /**
   * Get the IDs of newly created artifacts.
   */
  public synchronized List<UUID> getCreatedIds() {
    return created.stream().map(ArtifactNode::id).toList();
  }

  /**
   * Build a CommitDelta from these pending changes.
   */
  public synchronized CommitDelta toCommitDelta() {
    List<UUID> added = new ArrayList<>();
    List<ArtifactUpdate> updated = new ArrayList<>();

    for (ArtifactNode node : created) {
      if (node.supersedes() != null) {
        // This is a new version of an existing artifact
        updated.add(new ArtifactUpdate(
            node.getAnchorHash(),
            node.supersedes(),
            node.id()
        ));
      } else {
        // This is a brand new artifact
        added.add(node.id());
      }
    }

    return new CommitDelta(added, updated, List.of());
  }

  /**
   * Check if there are any changes.
   */
  public synchronized boolean hasChanges() {
    return !created.isEmpty() || !regenerating.isEmpty();
  }

  /**
   * Clear all pending changes (used on rollback).
   */
  public synchronized void clear() {
    regenerating.clear();
    keeping.clear();
    created.clear();
    relations.clear();
  }
}
