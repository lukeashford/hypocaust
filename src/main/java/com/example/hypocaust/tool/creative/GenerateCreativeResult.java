package com.example.hypocaust.tool.creative;

public record GenerateCreativeResult(
    String artifactName,
    String summary,
    String error
) implements ToolResult {

  @Override
  public boolean success() {
    return error == null;
  }

  public static GenerateCreativeResult success(String artifactName, String summary) {
    return new GenerateCreativeResult(artifactName, summary, null);
  }

  public static GenerateCreativeResult error(String error) {
    return new GenerateCreativeResult(null, null, error);
  }
}
