package com.example.hypocaust.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The execution context available to operators during a run.
 * Replaces the simple RunContextHolder with rich graph access.
 *
 * <p>This provides operators with visibility into existing artifacts and
 * allows them to reference artifacts by semantic anchors.
 *
 * @param projectId The project being executed
 * @param runId The current run ID
 * @param currentBranch The branch being executed on
 * @param parentCommit The commit at run start (null for first run)
 * @param graph The visible artifact graph (snapshot at run start)
 * @param pending Accumulator for this run's changes
 */
public record ExecutionContext(
    UUID projectId,
    UUID runId,
    Branch currentBranch,
    Commit parentCommit,
    ArtifactGraph graph,
    PendingChanges pending
) {

  public ExecutionContext {
    if (projectId == null) {
      throw new IllegalArgumentException("Project ID cannot be null");
    }
    if (runId == null) {
      throw new IllegalArgumentException("Run ID cannot be null");
    }
    if (currentBranch == null) {
      throw new IllegalArgumentException("Current branch cannot be null");
    }
    if (graph == null) {
      graph = ArtifactGraph.empty();
    }
    if (pending == null) {
      pending = new PendingChanges();
    }
  }

  /**
   * Create a new execution context for a first run (empty graph).
   */
  public static ExecutionContext forFirstRun(UUID projectId, UUID runId, Branch branch) {
    return new ExecutionContext(
        projectId,
        runId,
        branch,
        null,
        ArtifactGraph.empty(),
        new PendingChanges()
    );
  }

  /**
   * Create a new execution context with an existing graph.
   */
  public static ExecutionContext forRun(
      UUID projectId,
      UUID runId,
      Branch branch,
      Commit parentCommit,
      ArtifactGraph graph
  ) {
    return new ExecutionContext(
        projectId,
        runId,
        branch,
        parentCommit,
        graph,
        new PendingChanges()
    );
  }

  /**
   * Find artifacts by natural language query (semantic search on anchors).
   * This delegates to the graph's findByDescription method.
   *
   * @param query The search query
   * @return List of matching artifact nodes
   */
  public List<ArtifactNode> findByDescription(String query) {
    return graph.findByDescription(query);
  }

  /**
   * Get artifact by exact anchor.
   */
  public Optional<ArtifactNode> findByAnchor(SemanticAnchor anchor) {
    return graph.findByAnchor(anchor);
  }

  /**
   * Get artifact by role.
   */
  public Optional<ArtifactNode> findByRole(String role) {
    return graph.findByRole(role);
  }

  /**
   * Get artifact by ID.
   */
  public Optional<ArtifactNode> getArtifactById(UUID id) {
    return graph.getById(id);
  }

  /**
   * Mark an artifact for regeneration (will be superseded).
   */
  public void markForRegeneration(UUID artifactId) {
    pending.markForRegeneration(artifactId);
  }

  /**
   * Explicitly keep an artifact (won't be touched).
   */
  public void keep(UUID artifactId) {
    pending.keep(artifactId);
  }

  /**
   * Add a new artifact to pending changes.
   */
  public void addArtifact(ArtifactNode artifact) {
    pending.addCreated(artifact);
  }

  /**
   * Add a relation to pending changes.
   */
  public void addRelation(ArtifactRelation relation) {
    pending.addRelation(relation);
  }

  /**
   * Get all current artifacts visible in this context.
   */
  public List<ArtifactNode> getCurrentArtifacts() {
    return graph.getCurrentArtifacts();
  }

  /**
   * Check if the graph is empty (first run).
   */
  public boolean isFirstRun() {
    return graph.isEmpty() && parentCommit == null;
  }

  /**
   * Check if there are any pending changes.
   */
  public boolean hasPendingChanges() {
    return pending.hasChanges();
  }

  /**
   * Build a commit delta from pending changes.
   */
  public CommitDelta buildCommitDelta() {
    return pending.toCommitDelta();
  }
}
