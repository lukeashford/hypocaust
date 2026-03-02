package com.example.hypocaust.tool;

/**
 * Standard tool error result. Used by the tool execution bridge (ExecuteToolTool) to return
 * structured errors that the decomposer can parse.
 */
public record ToolError(
    String error,
    String message,
    String toolName
) implements ToolResult {

  @Override
  public boolean success() {
    return false;
  }

  @Override
  public String summary() {
    return null;
  }
}
