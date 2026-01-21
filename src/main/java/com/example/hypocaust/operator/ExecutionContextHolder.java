package com.example.hypocaust.operator;

import com.example.hypocaust.domain.ArtifactGraph;
import com.example.hypocaust.domain.ArtifactNode;
import com.example.hypocaust.domain.Branch;
import com.example.hypocaust.domain.Commit;
import com.example.hypocaust.domain.ExecutionContext;
import com.example.hypocaust.domain.PendingChanges;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the enhanced ExecutionContext.
 * This extends the functionality of RunContextHolder with artifact graph awareness.
 *
 * <p>Usage:
 * <pre>
 * ExecutionContextHolder.setContext(executionContext);
 * try {
 *   // Execute operators
 * } finally {
 *   ExecutionContextHolder.clear();
 * }
 * </pre>
 */
public final class ExecutionContextHolder {

  private static final ThreadLocal<ExecutionContext> contextHolder = new ThreadLocal<>();
  private static final ThreadLocal<Integer> operatorDepth = ThreadLocal.withInitial(() -> 0);

  private ExecutionContextHolder() {
    // Utility class
  }

  /**
   * Set the execution context for the current thread.
   */
  public static void setContext(ExecutionContext context) {
    if (context == null) {
      throw new IllegalArgumentException("ExecutionContext cannot be null");
    }
    contextHolder.set(context);
    operatorDepth.set(0);
    // Also set the legacy RunContextHolder for backward compatibility
    RunContextHolder.setContext(context.projectId(), context.runId());
  }

  /**
   * Create and set a context for a first run (empty graph).
   */
  public static void setContextForFirstRun(UUID projectId, UUID runId, Branch branch) {
    setContext(ExecutionContext.forFirstRun(projectId, runId, branch));
  }

  /**
   * Create and set a context for a run with an existing graph.
   */
  public static void setContextForRun(
      UUID projectId,
      UUID runId,
      Branch branch,
      Commit parentCommit,
      ArtifactGraph graph
  ) {
    setContext(ExecutionContext.forRun(projectId, runId, branch, parentCommit, graph));
  }

  /**
   * Get the current execution context.
   *
   * @throws IllegalStateException if no context is set
   */
  public static ExecutionContext getContext() {
    var context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("ExecutionContext not set for current thread");
    }
    return context;
  }

  /**
   * Get the current execution context if set.
   */
  public static Optional<ExecutionContext> getContextIfPresent() {
    return Optional.ofNullable(contextHolder.get());
  }

  /**
   * Check if a context is set.
   */
  public static boolean hasContext() {
    return contextHolder.get() != null;
  }

  /**
   * Get the project ID from the current context.
   */
  public static UUID getProjectId() {
    return getContext().projectId();
  }

  /**
   * Get the run ID from the current context.
   */
  public static UUID getRunId() {
    return getContext().runId();
  }

  /**
   * Get the current branch.
   */
  public static Branch getCurrentBranch() {
    return getContext().currentBranch();
  }

  /**
   * Get the artifact graph.
   */
  public static ArtifactGraph getGraph() {
    return getContext().graph();
  }

  /**
   * Get pending changes.
   */
  public static PendingChanges getPendingChanges() {
    return getContext().pending();
  }

  /**
   * Find artifacts by description query.
   */
  public static List<ArtifactNode> findByDescription(String query) {
    return getContext().findByDescription(query);
  }

  /**
   * Find artifact by role.
   */
  public static Optional<ArtifactNode> findByRole(String role) {
    return getContext().findByRole(role);
  }

  /**
   * Get artifact by ID.
   */
  public static Optional<ArtifactNode> getArtifactById(UUID id) {
    return getContext().getArtifactById(id);
  }

  /**
   * Mark an artifact for regeneration.
   */
  public static void markForRegeneration(UUID artifactId) {
    getContext().markForRegeneration(artifactId);
  }

  /**
   * Explicitly keep an artifact.
   */
  public static void keep(UUID artifactId) {
    getContext().keep(artifactId);
  }

  /**
   * Add a new artifact to pending changes.
   */
  public static void addArtifact(ArtifactNode artifact) {
    getContext().addArtifact(artifact);
  }

  /**
   * Get all current (non-superseded) artifacts.
   */
  public static List<ArtifactNode> getCurrentArtifacts() {
    return getContext().getCurrentArtifacts();
  }

  /**
   * Check if this is the first run (empty graph).
   */
  public static boolean isFirstRun() {
    return getContext().isFirstRun();
  }

  /**
   * Get the current operator depth (0 = top-level operator).
   */
  public static int getDepth() {
    return operatorDepth.get();
  }

  /**
   * Get indentation string for the current depth level.
   * Returns "  " (2 spaces) for each level of nesting.
   */
  public static String getIndent() {
    return "  ".repeat(operatorDepth.get());
  }

  /**
   * Increment the operator depth (call when entering a child operator).
   */
  public static void incrementDepth() {
    operatorDepth.set(operatorDepth.get() + 1);
    RunContextHolder.incrementDepth();
  }

  /**
   * Decrement the operator depth (call when exiting a child operator).
   */
  public static void decrementDepth() {
    var current = operatorDepth.get();
    if (current > 0) {
      operatorDepth.set(current - 1);
    }
    RunContextHolder.decrementDepth();
  }

  /**
   * Clear the context for the current thread.
   */
  public static void clear() {
    contextHolder.remove();
    operatorDepth.remove();
    RunContextHolder.clear();
  }
}
