package com.example.hypocaust.tool.creative;

import java.util.List;

public record GenerateCreativeResult(
    List<String> artifactNames,
    String summary,
    String error
) implements ToolResult {

  @Override
  public boolean success() {
    return error == null;
  }

  public static GenerateCreativeResult success(List<String> artifactNames, String summary) {
    return new GenerateCreativeResult(artifactNames, summary, null);
  }

  public static GenerateCreativeResult error(String error) {
    return new GenerateCreativeResult(null, null, error);
  }
}
