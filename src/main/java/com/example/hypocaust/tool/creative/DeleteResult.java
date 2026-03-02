package com.example.hypocaust.tool.creative;

import com.example.hypocaust.tool.ToolResult;

public record DeleteResult(
    String artifactName,
    String summary,
    String error
) implements ToolResult {

  public static DeleteResult success(String artifactName, String summary) {
    return new DeleteResult(artifactName, summary, null);
  }

  public static DeleteResult error(String error) {
    return new DeleteResult(null, null, error);
  }
}
