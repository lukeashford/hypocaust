package com.example.hypocaust.agent;

import java.util.List;

/**
 * Result of a decomposer execution. Returned by both root and child decomposers.
 * The parent receives the full result including artifact names for subsequent steps.
 */
public record DecomposerResult(
    boolean success,
    String summary,
    List<String> artifactNames,
    String errorMessage
) {

  public static DecomposerResult success(String summary, List<String> artifactNames) {
    return new DecomposerResult(true, summary, artifactNames != null ? artifactNames : List.of(),
        null);
  }

  public static DecomposerResult failure(String errorMessage) {
    return new DecomposerResult(false, null, List.of(), errorMessage);
  }
}
