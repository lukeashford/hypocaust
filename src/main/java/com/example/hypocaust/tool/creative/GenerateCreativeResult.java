package com.example.hypocaust.tool.creative;

public record GenerateCreativeResult(
    String artifactName,
    String summary,
    String error,
    ExecutionReport report
) implements ToolResult {

  @Override
  public boolean success() {
    return error == null;
  }

  public static GenerateCreativeResult success(
      String artifactName, String summary, ExecutionReport report) {
    return new GenerateCreativeResult(artifactName, summary, null, report);
  }

  public static GenerateCreativeResult error(String error, ExecutionReport report) {
    return new GenerateCreativeResult(null, null, error, report);
  }
}
