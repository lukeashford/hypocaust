package com.example.hypocaust.tool.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.tool.registry.ToolRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

class ExecuteToolToolTest {

  private ToolRegistry toolRegistry;
  private ExecuteToolTool executeToolTool;

  @BeforeEach
  void setUp() {
    toolRegistry = mock(ToolRegistry.class);
    executeToolTool = new ExecuteToolTool(toolRegistry);
  }

  @Test
  void execute_toolNotFound_returnsError() {
    when(toolRegistry.getCallback("nonexistent")).thenReturn(Optional.empty());

    var result = executeToolTool.execute("nonexistent", "{}");

    assertThat(result).contains("error").contains("Tool not found: nonexistent");
  }

  @Test
  void execute_successfulExecution_returnsResult() {
    var callback = mock(ToolCallback.class);
    when(callback.call("{\"task\":\"test\"}")).thenReturn("{\"result\":\"ok\"}");
    when(toolRegistry.getCallback("my_tool")).thenReturn(Optional.of(callback));

    var result = executeToolTool.execute("my_tool", "{\"task\":\"test\"}");

    assertThat(result).isEqualTo("{\"result\":\"ok\"}");
  }

  @Test
  void execute_callbackThrows_returnsError() {
    var callback = mock(ToolCallback.class);
    when(callback.call("{}")).thenThrow(new RuntimeException("Connection refused"));
    when(toolRegistry.getCallback("failing_tool")).thenReturn(Optional.of(callback));

    var result = executeToolTool.execute("failing_tool", "{}");

    assertThat(result).contains("error").contains("Connection refused");
  }
}
