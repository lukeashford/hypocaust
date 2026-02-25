package com.example.hypocaust.models;

import com.example.hypocaust.domain.ArtifactKind;
import com.fasterxml.jackson.databind.JsonNode;

public interface ModelExecutor {

  Platform platform();

  ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices);

  JsonNode execute(String owner, String modelId, JsonNode input);

  /**
   * Executes with automatic retry on transient failures (network errors, 5xx). Returns structured
   * attempt metadata for upstream reporting. Implementations that extend
   * {@link AbstractModelExecutor} inherit this; others may override.
   */
  ExecutionAttempt executeWithRetry(String owner, String modelId, JsonNode input);

  String extractOutput(JsonNode output);
}
