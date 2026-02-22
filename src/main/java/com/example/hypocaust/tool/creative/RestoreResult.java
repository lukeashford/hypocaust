package com.example.hypocaust.tool.creative;

public record RestoreResult(
    String originalName,
    String restoredName,
    String executionName,
    String summary,
    String error
) {

  public static RestoreResult success(
      String originalName, String restoredName, String executionName, String summary) {
    return new RestoreResult(originalName, restoredName, executionName, summary, null);
  }

  public static RestoreResult error(String error) {
    return new RestoreResult(null, null, null, null, error);
  }
}
