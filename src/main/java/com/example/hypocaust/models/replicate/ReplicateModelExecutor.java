package com.example.hypocaust.models.replicate;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
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
      var fullSchema = replicateClient.getSchema(owner, modelId, version);

      // Extract the specific input schema to reduce noise and ambiguity
      var inputSchema = fullSchema.path("components").path("schemas").path("Input");
      if (inputSchema.isMissingNode()) {
        // Fallback to the full schema if "Input" schema isn't where we expect it
        inputSchema = fullSchema;
      }

      log.info("Fetched input schema for {}/{}: {}", owner, modelId, inputSchema);
      var modelDocs = description + "\n\nBest Practices:\n" + bestPractices;
      return String.format("Model Docs: %s\nSchema: %s", modelDocs, inputSchema);
    } catch (Exception e) {
      log.warn("Failed to fetch Replicate model context for {}/{}: {}", owner, modelId,
          e.getMessage());
      var modelDocs = description + "\n\nBest Practices:\n" + bestPractices;
      return "Model Docs: " + modelDocs + "\nSchema: unavailable";
    }
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    var version = replicateClient.getLatestVersion(owner, modelId);
    return replicateClient.predict(owner, modelId, version, input);
  }

  @Override
  public String extractOutput(JsonNode output) {
    if (output.isTextual()) {
      return output.asText();
    }
    if (output.isArray() && !output.isEmpty()) {
      String first = output.get(0).asText();
      if (isUrl(first)) {
        return first; // Take the first generated image
      } else {
        // Join tokens for LLM/Text models
        StringBuilder sb = new StringBuilder();
        output.forEach(node -> sb.append(node.asText()));
        return sb.toString();
      }
    }
    if (output.has("url")) {
      return output.get("url").asText();
    }
    return output.toString();
  }

  private boolean isUrl(String s) {
    return s != null && (s.startsWith("http://") || s.startsWith("https://"));
  }
}
