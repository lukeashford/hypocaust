package com.example.hypocaust.agent;

import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoStatus;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Orchestrates the execution of a task (top-level or subtask), managing the todo lifecycle,
 * thread-local stack, and depth tracking.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TodoExecutor {

  private static final String LOG_PREFIX = "[TODO] ";

  /**
   * Execute a piece of work as a task with a human-readable description.
   *
   * @param description The label for the todo representing this task
   * @param work The work to execute
   * @param <T> The return type of the work
   * @return The result of the work
   */
  public <T> T execute(String description, Supplier<T> work) {

    TaskExecutionContextHolder.incrementDepth();
    UUID parentTodoId = TaskExecutionContextHolder.getCurrentTodoId();
    String indent = TaskExecutionContextHolder.getIndent();

    // Create or find a todo for this task
    Todo todo = TaskExecutionContextHolder.getTodos().addOrUpdateTodo(
        parentTodoId, new Todo(description, TodoStatus.PENDING));

    log.info("{}{} Starting: {}", indent, LOG_PREFIX, description);

    TaskExecutionContextHolder.pushTodoId(todo.id());
    TaskExecutionContextHolder.markCurrentTodoRunning();

    try {
      T result = work.get();

      // Handle success/failure based on DecomposerResult if that's what we got
      if (result instanceof DecomposerResult dr) {
        if (dr.success()) {
          TaskExecutionContextHolder.markCurrentTodoCompleted();
          log.info("{}{} Completed: {}", indent, LOG_PREFIX, description);
        } else {
          TaskExecutionContextHolder.markCurrentTodoFailed();
          log.warn("{}{} Failed: {} - {}", indent, LOG_PREFIX, description, dr.errorMessage());
        }
      } else {
        // For other types of work, assume success if no exception was thrown
        TaskExecutionContextHolder.markCurrentTodoCompleted();
        log.info("{}{} Completed: {}", indent, LOG_PREFIX, description);
      }

      return result;
    } catch (Exception e) {
      TaskExecutionContextHolder.markCurrentTodoFailed();
      log.error("{}{} Error: {} - {}", indent, LOG_PREFIX, description, e.getMessage(), e);
      throw e;
    } finally {
      TaskExecutionContextHolder.popTodoId();
      TaskExecutionContextHolder.decrementDepth();
    }
  }
}
