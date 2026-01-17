package com.example.hypocaust.operator;

import java.util.UUID;

public final class RunContextHolder {

  private static final ThreadLocal<RunExecutionContext> contextHolder = new ThreadLocal<>();
  private static final ThreadLocal<Integer> operatorDepth = ThreadLocal.withInitial(() -> 0);

  private RunContextHolder() {
    // Utility class
  }

  public static void setContext(UUID projectId, UUID runId) {
    contextHolder.set(new RunExecutionContext(projectId, runId));
    operatorDepth.set(0);
  }

  public static void setContext(RunExecutionContext context) {
    contextHolder.set(context);
  }

  public static RunExecutionContext getContext() {
    final var context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("OperatorExecutionContext not set for current thread");
    }
    return context;
  }

  public static UUID getProjectId() {
    return getContext().projectId();
  }

  public static UUID getRunId() {
    return getContext().runId();
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
  }

  /**
   * Decrement the operator depth (call when exiting a child operator).
   */
  public static void decrementDepth() {
    final var current = operatorDepth.get();
    if (current > 0) {
      operatorDepth.set(current - 1);
    }
  }

  public static void clear() {
    contextHolder.remove();
    operatorDepth.remove();
  }

  public record RunExecutionContext(UUID projectId, UUID runId) {

  }
}