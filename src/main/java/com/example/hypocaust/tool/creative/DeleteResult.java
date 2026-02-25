package com.example.hypocaust.tool.creative;

public record DeleteResult(
    String artifactName,
    String summary,
    String error,
    ExecutionReport report
) implements ToolResult {

  @Override
  public boolean success() {
    return error == null;
  }

  public static DeleteResult success(String artifactName, String summary) {
    var report = ExecutionReport.builder()
        .success(true)
        .summary(summary)
        .addArtifactName(artifactName)
        .build();
    return new DeleteResult(artifactName, summary, null, report);
  }

  public static DeleteResult error(String error) {
    var report = ExecutionReport.builder()
        .success(false)
        .summary(error)
        .build();
    return new DeleteResult(null, null, error, report);
  }
}
