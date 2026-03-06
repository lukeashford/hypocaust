package com.example.hypocaust.tool.discovery;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.tool.ToolError;
import com.example.hypocaust.tool.registry.ToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Bridge tool that invokes discoverable tools by name. The decomposer discovers tools via
 * {@link SearchToolsTool} and calls them through this bridge.
 *
 * <p>Leverages Spring AI's {@code ToolCallback} infrastructure for type-safe parameter
 * deserialization and return type serialization.
 *
 * <p><b>Delegation enforcement:</b> If the current decomposer declared a plan (via {@code
 * set_plan}) with more than one step, direct tool execution is blocked. The decomposer must use
 * {@code invoke_decomposer} instead, ensuring context isolation per subtask.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecuteToolTool {

  private final ToolRegistry toolRegistry;
  private final ObjectMapper objectMapper;

  @Tool(name = "execute_tool",
      description = "Execute a tool discovered via search_tools by name with JSON parameters. "
          + "This is the ONLY way to call dynamically discovered tools.")
  public String execute(
      @ToolParam(description = "The name of the tool to execute") String toolName,
      @ToolParam(description = "JSON object with the tool's parameters") String parametersJson
  ) {
    // Enforce delegation: if the current decomposer's plan has >1 step, block direct execution.
    UUID currentTodoId = TaskExecutionContextHolder.getCurrentTodoId();
    int planSteps = TaskExecutionContextHolder.getTodos().getChildCount(currentTodoId);
    if (planSteps > 1) {
      log.warn("{} [EXECUTE_TOOL] Blocked: plan has {} steps, must use invoke_decomposer",
          TaskExecutionContextHolder.getIndent(), planSteps);
      return serialize(new ToolError("DELEGATION_REQUIRED",
          "Your plan has " + planSteps + " steps. "
              + "When a task has multiple steps, you MUST delegate each step to a child "
              + "via invoke_decomposer. Direct execute_tool calls are only allowed for "
              + "single-step tasks.", null));
    }

    log.info("{} [EXECUTE_TOOL] Executing: {} with params: {}",
        TaskExecutionContextHolder.getIndent(), toolName, parametersJson);

    var callbackOpt = toolRegistry.getCallback(toolName);
    if (callbackOpt.isEmpty()) {
      log.warn("{} [EXECUTE_TOOL] Tool not found: {}", TaskExecutionContextHolder.getIndent(),
          toolName);
      return serialize(new ToolError("TOOL_NOT_FOUND", "Tool not found: " + toolName, toolName));
    }

    var callback = callbackOpt.get();
    try {
      var result = callback.call(parametersJson);
      log.debug("{} [EXECUTE_TOOL] Success: {}", TaskExecutionContextHolder.getIndent(), toolName);
      return result;
    } catch (Exception e) {
      String message = e.getMessage();
      if (message == null) {
        message = e.getClass().getSimpleName();
      }
      log.error("{} [EXECUTE_TOOL] Failed: {}: {}", TaskExecutionContextHolder.getIndent(),
          toolName, message);
      return serialize(new ToolError("EXECUTION_FAILED", message, toolName));
    }
  }

  private String serialize(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      log.error("{} [EXECUTE_TOOL] Serialization failed", TaskExecutionContextHolder.getIndent(),
          e);
      return "{\"error\": \"SERIALIZATION_ERROR\", \"message\": \"" + e.getMessage() + "\"}";
    }
  }
}
