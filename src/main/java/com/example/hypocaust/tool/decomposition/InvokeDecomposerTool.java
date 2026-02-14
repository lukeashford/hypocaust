package com.example.hypocaust.tool.decomposition;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoStatus;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Tool for recursive decomposer invocation. Spawns a child decomposer with a fresh
 * context, managing the todo lifecycle and depth tracking.
 *
 * <p>The child's entire conversation history is discarded. Only the {@link DecomposerResult}
 * crosses the boundary. This is the context isolation mechanism.
 */
@Component
@Slf4j
public class InvokeDecomposerTool {

  private final Decomposer decomposer;

  public InvokeDecomposerTool(@Lazy Decomposer decomposer) {
    this.decomposer = decomposer;
  }

  @Tool(name = "invoke_decomposer",
      description = "Delegate a subtask to a child decomposer agent. "
          + "The child operates in its own isolated context and returns a result.")
  public DecomposerResult invoke(
      @ToolParam(description = "Self-contained task description with all necessary context") String task,
      @ToolParam(description = "Human-readable progress label for this step") String todoDescription
  ) {
    var parentTodoId = TaskExecutionContextHolder.getCurrentTodoId();
    var indent = TaskExecutionContextHolder.getIndent();

    // Create a todo for this subtask
    var todo = new Todo(todoDescription != null ? todoDescription : task, TodoStatus.PENDING);
    if (parentTodoId != null) {
      TaskExecutionContextHolder.getTodos().registerSubtodos(parentTodoId, List.of(todo));
    }

    log.info("{}[CHILD] Starting: {}", indent, todoDescription);

    TaskExecutionContextHolder.pushTodoId(todo.id());
    TaskExecutionContextHolder.markCurrentTodoRunning();
    TaskExecutionContextHolder.incrementDepth();

    try {
      var result = decomposer.execute(task);

      if (result.success()) {
        TaskExecutionContextHolder.markCurrentTodoCompleted();
        log.info("{}[CHILD] Completed: {}", indent, todoDescription);
      } else {
        TaskExecutionContextHolder.markCurrentTodoFailed();
        log.warn("{}[CHILD] Failed: {} - {}", indent, todoDescription, result.errorMessage());
      }

      return result;
    } catch (Exception e) {
      TaskExecutionContextHolder.markCurrentTodoFailed();
      log.error("{}[CHILD] Error: {} - {}", indent, todoDescription, e.getMessage(), e);
      return DecomposerResult.failure(e.getMessage());
    } finally {
      TaskExecutionContextHolder.decrementDepth();
      TaskExecutionContextHolder.popTodoId();
    }
  }
}
