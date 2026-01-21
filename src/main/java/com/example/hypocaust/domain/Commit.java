package com.example.hypocaust.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an immutable snapshot in the project's version history.
 * Each run that completes successfully produces a commit.
 *
 * @param id Unique identifier for this commit
 * @param branchId The branch this commit belongs to
 * @param parentCommitId The previous commit on this branch (null for first commit)
 * @param runId The run that produced this commit
 * @param task The verbatim task that was executed (for visualization)
 * @param timestamp When this commit was created
 * @param delta The changes made in this commit
 */
public record Commit(
    UUID id,
    UUID branchId,
    UUID parentCommitId,
    UUID runId,
    String task,
    Instant timestamp,
    CommitDelta delta
) {

  public Commit {
    if (id == null) {
      throw new IllegalArgumentException("Commit ID cannot be null");
    }
    if (branchId == null) {
      throw new IllegalArgumentException("Branch ID cannot be null");
    }
    if (runId == null) {
      throw new IllegalArgumentException("Run ID cannot be null");
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
    if (delta == null) {
      delta = CommitDelta.empty();
    }
  }

  /**
   * Create a new commit for a branch.
   */
  public static Commit create(
      UUID id,
      UUID branchId,
      UUID parentCommitId,
      UUID runId,
      String task,
      CommitDelta delta
  ) {
    return new Commit(id, branchId, parentCommitId, runId, task, Instant.now(), delta);
  }

  /**
   * Check if this is the first commit on the branch.
   */
  public boolean isInitial() {
    return parentCommitId == null;
  }

  /**
   * Check if this commit has any changes.
   */
  public boolean hasChanges() {
    return !delta.isEmpty();
  }
}
