package com.example.hypocaust.operator;

import java.util.UUID;

public final class RunContextHolder {

  private static final ThreadLocal<RunExecutionContext> contextHolder = new ThreadLocal<>();

  private RunContextHolder() {
    // Utility class
  }

  public static void setContext(UUID projectId, UUID runId) {
    contextHolder.set(new RunExecutionContext(projectId, runId));
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

  public static void clear() {
    contextHolder.remove();
  }

  public record RunExecutionContext(UUID projectId, UUID runId) {

  }
}