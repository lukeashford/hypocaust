package com.example.hypocaust.models;

import com.example.hypocaust.domain.ArtifactKind;
import com.fasterxml.jackson.databind.JsonNode;

public interface ModelExecutor {

  Platform platform();

  ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices);

  JsonNode execute(String owner, String modelId, JsonNode input);

  String extractOutput(JsonNode output);
}
