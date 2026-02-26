package com.example.hypocaust.tool.discovery;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.tool.registry.ToolRegistry;
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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExecuteToolTool {

  private final ToolRegistry toolRegistry;

  @Tool(name = "execute_tool",
      description = "Execute a discovered tool by name with the given parameters.")
  public String execute(
      @ToolParam(description = "The name of the tool to execute") String toolName,
      @ToolParam(description = "JSON object with the tool's parameters") String parametersJson
  ) {
    log.info("{} [EXECUTE_TOOL] Executing: {} with params: {}",
        TaskExecutionContextHolder.getIndent(), toolName, parametersJson);
    var callbackOpt = toolRegistry.getCallback(toolName);
    if (callbackOpt.isEmpty()) {
      log.warn("{} [EXECUTE_TOOL] Tool not found: {}", TaskExecutionContextHolder.getIndent(),
          toolName);
      return "{\"error\": \"Tool not found: " + toolName + "\"}";
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
      return "{\"error\": \"" + message.replace("\"", "'") + "\", \"toolName\": \""
          + toolName + "\"}";
    }
  }
}
