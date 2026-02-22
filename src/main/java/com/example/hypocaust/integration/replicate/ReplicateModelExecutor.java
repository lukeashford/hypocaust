package com.example.hypocaust.integration.replicate;

import com.example.hypocaust.integration.AbstractModelExecutor;
import com.example.hypocaust.integration.Platform;
import com.example.hypocaust.integration.ReplicateClient;
import com.example.hypocaust.models.ModelRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReplicateModelExecutor extends AbstractModelExecutor {

  private final ReplicateClient replicateClient;

  public ReplicateModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ReplicateClient replicateClient) {
    super(modelRegistry, objectMapper);
    this.replicateClient = replicateClient;
  }

  @Override
  public Platform platform() {
    return Platform.REPLICATE;
  }

  @Override
  protected String planSystemPrompt() {
    return """
        YOUR RESPONSIBILITIES:
        1. Input Mapping: Construct the 'providerInput' object matching the provided OpenAPI schema.
           - Optimize prompts for the best artistic results.
           - Map user requirements to specific schema fields.
           - If a field requires a URL/image and the user refers to an artifact, use '@artifact_name' as a placeholder.
        2. Validation:
           - Ensure all REQUIRED fields in the schema are present.
           - If the user task is missing information that is MANDATORY for the model and cannot be reasonably defaulted, \
             provide a concise but precise 'errorMessage' explaining what's missing (e.g., "This model requires a video length").
           - If you provide an 'errorMessage', 'providerInput' should be null.
        """;
  }

  @Override
  protected String additionalPlanContext(String owner, String modelId,
      String description, String bestPractices) {
    try {
      var version = replicateClient.getLatestVersion(owner, modelId);
      var schema = replicateClient.getSchema(owner, modelId, version);
      var modelDocs = description + "\n\nBest Practices:\n" + bestPractices;
      return String.format("Model Docs: %s\nSchema: %s", modelDocs, schema);
    } catch (Exception e) {
      log.warn("Failed to fetch Replicate model context for {}/{}: {}", owner, modelId,
          e.getMessage());
      var modelDocs = description + "\n\nBest Practices:\n" + bestPractices;
      return "Model Docs: " + modelDocs + "\nSchema: unavailable";
    }
  }

  @Override
  public JsonNode execute(String owner, String modelId, JsonNode input) {
    var version = replicateClient.getLatestVersion(owner, modelId);
    return replicateClient.predict(owner, modelId, version, input);
  }

  @Override
  public String extractOutputUrl(JsonNode output) {
    if (output.isTextual()) {
      return output.asText();
    }
    if (output.isArray() && !output.isEmpty()) {
      return output.get(0).asText();
    }
    if (output.has("url")) {
      return output.get("url").asText();
    }
    return output.toString();
  }
}
