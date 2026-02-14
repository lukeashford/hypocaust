package com.example.hypocaust.tool.creative;

public record DeleteResult(String artifactName, String summary, String error) {

  public static DeleteResult success(String artifactName, String summary) {
    return new DeleteResult(artifactName, summary, null);
  }

  public static DeleteResult error(String error) {
    return new DeleteResult(null, null, error);
  }
}
