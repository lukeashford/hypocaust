package com.example.hypocaust.tool.creative;

import com.example.hypocaust.tool.ToolResult;

public record RestoreResult(
    String originalName,
    String restoredName,
    String executionName,
    String summary,
    String error
) implements ToolResult {

  public static RestoreResult success(
      String originalName, String restoredName, String executionName, String summary) {
    return new RestoreResult(originalName, restoredName, executionName, summary, null);
  }

  public static RestoreResult error(String error) {
    return new RestoreResult(null, null, null, null, error);
  }
}
