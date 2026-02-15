package com.example.hypocaust.tool.discovery;

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
    var callbackOpt = toolRegistry.getCallback(toolName);
    if (callbackOpt.isEmpty()) {
      return "{\"error\": \"Tool not found: " + toolName + "\"}";
    }

    var callback = callbackOpt.get();
    try {
      return callback.call(parametersJson);
    } catch (Exception e) {
      String message = e.getMessage();
      if (message == null) {
        message = e.getClass().getSimpleName();
      }
      log.error("Tool execution failed for {}: {}", toolName, message, e);
      return "{\"error\": \"" + message.replace("\"", "'") + "\", \"toolName\": \""
          + toolName + "\"}";
    }
  }
}
