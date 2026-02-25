package com.example.hypocaust.tool.creative;

public record RestoreResult(
    String originalName,
    String restoredName,
    String executionName,
    String summary,
    String error,
    ExecutionReport report
) implements ToolResult {

  @Override
  public boolean success() {
    return error == null;
  }

  public static RestoreResult success(
      String originalName, String restoredName, String executionName, String summary) {
    var report = ExecutionReport.builder()
        .success(true)
        .summary(summary)
        .addArtifactName(restoredName)
        .build();
    return new RestoreResult(originalName, restoredName, executionName, summary, null, report);
  }

  public static RestoreResult error(String error) {
    var report = ExecutionReport.builder()
        .success(false)
        .summary(error)
        .build();
    return new RestoreResult(null, null, null, null, error, report);
  }
}
