package com.example.hypocaust.domain;

import java.util.List;
import java.util.UUID;

/**
 * Represents the changes made in a commit.
 *
 * @param added IDs of newly created artifacts
 * @param updated Details of artifacts that were modified (new versions created)
 * @param removed IDs of artifacts that were removed (rare)
 */
public record CommitDelta(
    List<UUID> added,
    List<ArtifactUpdate> updated,
    List<UUID> removed
) {

  public CommitDelta {
    added = added != null ? List.copyOf(added) : List.of();
    updated = updated != null ? List.copyOf(updated) : List.of();
    removed = removed != null ? List.copyOf(removed) : List.of();
  }

  /**
   * Create an empty delta.
   */
  public static CommitDelta empty() {
    return new CommitDelta(List.of(), List.of(), List.of());
  }

  /**
   * Create a delta with only added artifacts.
   */
  public static CommitDelta added(List<UUID> addedIds) {
    return new CommitDelta(addedIds, List.of(), List.of());
  }

  /**
   * Check if this delta is empty (no changes).
   */
  public boolean isEmpty() {
    return added.isEmpty() && updated.isEmpty() && removed.isEmpty();
  }

  /**
   * Get the total number of changes.
   */
  public int changeCount() {
    return added.size() + updated.size() + removed.size();
  }
}
