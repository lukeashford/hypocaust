package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface ModelExecutor {

  Platform platform();

  ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices,
      List<Artifact> availableArtifacts);

  JsonNode execute(String owner, String modelId, JsonNode input);

  String extractOutputUrl(JsonNode output);
}
