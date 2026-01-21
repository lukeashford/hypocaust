package com.example.hypocaust.domain;

import java.util.UUID;

/**
 * Represents a branch in the project's version history.
 * Each project can have multiple branches (like git) for exploring alternatives.
 *
 * @param id Unique identifier for this branch
 * @param projectId The project this branch belongs to
 * @param name Human-readable name (e.g., "main", "blonde-variant")
 * @param headCommitId The current head commit of this branch (null if no commits yet)
 * @param parentBranchId The branch this was forked from (null for the initial branch)
 */
public record Branch(
    UUID id,
    UUID projectId,
    String name,
    UUID headCommitId,
    UUID parentBranchId
) {

  public Branch {
    if (id == null) {
      throw new IllegalArgumentException("Branch ID cannot be null");
    }
    if (projectId == null) {
      throw new IllegalArgumentException("Project ID cannot be null");
    }
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Branch name cannot be null or blank");
    }
  }

  /**
   * Create a new main branch for a project.
   */
  public static Branch main(UUID id, UUID projectId) {
    return new Branch(id, projectId, "main", null, null);
  }

  /**
   * Create a new branch forked from this one.
   */
  public Branch fork(UUID newId, String newName) {
    return new Branch(newId, projectId, newName, headCommitId, id);
  }

  /**
   * Create a copy of this branch with an updated head commit.
   */
  public Branch withHeadCommit(UUID newHeadCommitId) {
    return new Branch(id, projectId, name, newHeadCommitId, parentBranchId);
  }

  /**
   * Check if this is the main branch.
   */
  public boolean isMain() {
    return "main".equals(name);
  }

  /**
   * Check if this branch has any commits.
   */
  public boolean hasCommits() {
    return headCommitId != null;
  }
}
