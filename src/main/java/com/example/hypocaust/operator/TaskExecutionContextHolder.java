package com.example.hypocaust.operator;

import com.example.hypocaust.domain.PendingArtifact;
import com.example.hypocaust.domain.TaskExecutionContext;
import java.util.UUID;

/**
 * Thread-local holder for TaskExecutionContext. Provides convenience methods for common
 * operations.
 */
public final class TaskExecutionContextHolder {

  private static final ThreadLocal<TaskExecutionContext> contextHolder = new ThreadLocal<>();
  private static final ThreadLocal<Integer> operatorDepth = ThreadLocal.withInitial(() -> 0);

  private TaskExecutionContextHolder() {
    // Utility class
  }

  public static void setContext(TaskExecutionContext ctx) {
    contextHolder.set(ctx);
    operatorDepth.set(0);
  }

  public static TaskExecutionContext getContext() {
    TaskExecutionContext context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("TaskExecutionContext not set for current thread");
    }
    return context;
  }

  public static boolean hasContext() {
    return contextHolder.get() != null;
  }

  public static void clear() {
    contextHolder.remove();
    operatorDepth.remove();
  }

  // === Convenience methods ===

  public static UUID getProjectId() {
    return getContext().getProjectId();
  }

  public static UUID getTaskExecutionId() {
    return getContext().getTaskExecutionId();
  }

  public static UUID getPredecessorId() {
    return getContext().getPredecessorId();
  }

  /**
   * Schedule a new artifact for creation.
   *
   * @return the generated artifact fileName
   */
  public static String addArtifact(PendingArtifact artifact) {
    return getContext().addArtifact(artifact);
  }

  /**
   * Schedule an edit to an existing artifact.
   */
  public static void editArtifact(String name, PendingArtifact newVersion) {
    getContext().editArtifact(name, newVersion);
  }

  /**
   * Schedule an artifact for deletion.
   */
  public static void deleteArtifact(String name) {
    getContext().deleteArtifact(name);
  }

  /**
   * Update a pending artifact.
   */
  public static void updatePendingArtifact(String name, PendingArtifact newVersion) {
    getContext().updatePendingArtifact(name, newVersion);
  }

  /**
   * Cancel a pending artifact.
   */
  public static void cancelPendingArtifact(String name) {
    getContext().cancelPendingArtifact(name);
  }

  /**
   * Check if an artifact exists.
   */
  public static boolean artifactExists(String name) {
    return getContext().artifactExists(name);
  }

  // === Operator depth tracking (for logging indentation) ===

  /**
   * Get the current operator depth (0 = top-level operator).
   */
  public static int getDepth() {
    return operatorDepth.get();
  }

  /**
   * Get indentation string for the current depth level. Returns "  " (2 spaces) for each level of
   * nesting.
   */
  public static String getIndent() {
    return "  ".repeat(operatorDepth.get());
  }

  /**
   * Increment the operator depth (call when entering a child operator).
   */
  public static void incrementDepth() {
    operatorDepth.set(operatorDepth.get() + 1);
  }

  /**
   * Decrement the operator depth (call when exiting a child operator).
   */
  public static void decrementDepth() {
    int current = operatorDepth.get();
    if (current > 0) {
      operatorDepth.set(current - 1);
    }
  }
}
