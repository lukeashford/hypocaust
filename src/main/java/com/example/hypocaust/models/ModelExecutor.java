package com.example.hypocaust.models;

import com.example.hypocaust.domain.ArtifactKind;
import com.fasterxml.jackson.databind.JsonNode;

public interface ModelExecutor {

  Platform platform();

  ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices);

  /**
   * Plans and executes the model call in one go. Planning is NOT retried (secured by ChatService),
   * but execution IS retried on transient failures.
   */
  JsonNode planAndExecute(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices);

  /**
   * Executes the model call. Implementations in {@link AbstractModelExecutor} automatically retry
   * transient failures; throws on permanent failure.
   */
  JsonNode execute(String owner, String modelId, JsonNode input);

  String extractOutput(JsonNode output);
}
