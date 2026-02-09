package com.example.hypocaust.operator;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TodosContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-local holder for TaskExecutionContext. Provides convenience methods for common
 * operations.
 */
public final class TaskExecutionContextHolder {

  private static final ThreadLocal<TaskExecutionContext> contextHolder = new ThreadLocal<>();
  private static final ThreadLocal<Integer> operatorDepth = ThreadLocal.withInitial(() -> 0);
  private static final ThreadLocal<Deque<UUID>> todoPath = ThreadLocal.withInitial(ArrayDeque::new);
  private static final ConcurrentHashMap<UUID, TaskExecutionContext> contextsByExecution = new ConcurrentHashMap<>();

  private TaskExecutionContextHolder() {
    // Utility class
  }

  public static void setContext(TaskExecutionContext ctx) {
    contextHolder.set(ctx);
    operatorDepth.set(0);
    // Register for cross-thread lookup
    contextsByExecution.put(ctx.getTaskExecutionId(), ctx);
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
    TaskExecutionContext ctx = contextHolder.get();
    if (ctx != null) {
      contextsByExecution.remove(ctx.getTaskExecutionId());
    }
    contextHolder.remove();
    operatorDepth.remove();
    todoPath.remove();
  }

  // New method for cross-thread access
  public static Optional<TaskExecutionContext> getContextByTaskExecutionId(UUID taskExecutionId) {
    return Optional.ofNullable(contextsByExecution.get(taskExecutionId));
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
   * @return the generated artifact name
   */
  public static String addArtifact(ArtifactDraft draft) {
    return getContext().getArtifacts().add(draft);
  }

  /**
   * Schedule an edit to an existing artifact.
   */
  public static void editArtifact(Artifact newVersion) {
    getContext().getArtifacts().edit(newVersion);
  }

  /**
   * Schedule an artifact for deletion.
   */
  public static void deleteArtifact(String name) {
    getContext().getArtifacts().delete(name);
  }

  /**
   * Update a pending artifact.
   */
  public static void updateArtifact(Artifact newVersion) {
    getContext().getArtifacts().updatePending(newVersion);
  }

  /**
   * Rollback a pending artifact (removes from changelist entirely).
   */
  public static void rollbackArtifact(String name) {
    getContext().getArtifacts().rollbackPending(name);
  }

  /**
   * Check if an artifact exists.
   */
  public static boolean artifactExists(String name) {
    return getContext().getArtifacts().exists(name);
  }

  // === Todo lifecycle convenience methods ===

  public static TodosContext getTodos() {
    return getContext().getTodos();
  }

  public static void markCurrentTodoRunning() {
    getTodos().markRunning(getCurrentTodoId());
  }

  public static void markCurrentTodoCompleted() {
    getTodos().markCompleted(getCurrentTodoId());
  }

  public static void markCurrentTodoFailed() {
    getTodos().markFailed(getCurrentTodoId());
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

  // === Todo path tracking ===

  /**
   * Get the current todo ID from the stack.
   */
  public static UUID getCurrentTodoId() {
    return todoPath.get().peek();
  }

  /**
   * Push a todo ID onto the stack.
   */
  public static void pushTodoId(UUID todoId) {
    if (todoId != null) {
      todoPath.get().push(todoId);
    }
  }

  /**
   * Pop a todo ID from the stack.
   */
  public static void popTodoId() {
    Deque<UUID> stack = todoPath.get();
    if (!stack.isEmpty()) {
      stack.pop();
    }
  }
}
