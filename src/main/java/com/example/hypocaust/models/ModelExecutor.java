package com.example.hypocaust.models;

import com.example.hypocaust.domain.ArtifactKind;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.UnaryOperator;

public interface ModelExecutor {

  Platform platform();

  /**
   * Runs the full executor pipeline: plan the provider input, transform it (e.g. substitute
   * artifact placeholders), execute the model call with retries, and extract the final output.
   */
  ExecutionResult run(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices,
      UnaryOperator<JsonNode> inputTransformer);
}
