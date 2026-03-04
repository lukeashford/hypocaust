package com.example.hypocaust.tool.decomposition;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TodoExecutor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Tool for recursive decomposer invocation. Spawns a child decomposer with a fresh context,
 * managing the todo lifecycle and depth tracking.
 *
 * <p>The child's entire conversation history is discarded. Only the {@link DecomposerResult}
 * crosses the boundary. This is the context isolation mechanism.
 */
@Component
@Slf4j
public class InvokeDecomposerTool {

  private final Decomposer decomposer;
  private final TodoExecutor todoExecutor;

  public InvokeDecomposerTool(@Lazy Decomposer decomposer, TodoExecutor todoExecutor) {
    this.decomposer = decomposer;
    this.todoExecutor = todoExecutor;
  }

  @Tool(name = "invoke_decomposer",
      description = "Delegate a subtask to a child decomposer agent. "
          + "The child operates in its own isolated context and returns a result.")
  public DecomposerResult invoke(
      @ToolParam(description = "Self-contained task description with all necessary context") String task,
      @ToolParam(description = "Human-readable progress label for this step") String todoDescription,
      @ToolParam(description = "Key facts established so far that this subtask needs. "
          + "3-5 bullet points of context: character descriptions, style decisions, "
          + "artifact names to reference, constraints the user stated.",
          required = false) List<String> contextBrief
  ) {
    try {
      return todoExecutor.execute(
          todoDescription != null ? todoDescription : task,
          () -> decomposer.execute(task, contextBrief)
      );
    } catch (Exception e) {
      // TaskExecutor already logged and marked failed.
      return DecomposerResult.failure(e.getMessage());
    }
  }
}
