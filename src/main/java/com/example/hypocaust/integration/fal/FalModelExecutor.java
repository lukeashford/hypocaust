package com.example.hypocaust.integration.fal;

import com.example.hypocaust.integration.AbstractModelExecutor;
import com.example.hypocaust.integration.Platform;
import com.example.hypocaust.models.ModelRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.fal.api-key")
@Slf4j
public class FalModelExecutor extends AbstractModelExecutor {

  private final FalClient falClient;

  public FalModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      FalClient falClient) {
    super(modelRegistry, objectMapper);
    this.falClient = falClient;
  }

  @Override
  public Platform platform() {
    return Platform.FAL;
  }

  @Override
  protected String planSystemPrompt() {
    return """
        You are an expert creative director and prompt engineer. Your goal is to prepare a generation plan \
        for a fal.ai model based on a user's task.

        INPUTS PROVIDED:
        1. User Task: The natural language description of what to generate/edit.
        2. Model Docs: Contextual information about the selected model and its input format.
        3. Available Artifacts: Names of artifacts currently in the project.

        YOUR RESPONSIBILITIES:
        1. Input Mapping: Construct the 'providerInput' object matching the fal.ai model's expected input format.
           - Optimize prompts for the best artistic results.
           - If a field requires a URL/image and the user refers to an artifact, use '@artifact_name' as a placeholder.
        2. Validation:
           - If the user task is missing information that is MANDATORY for the model, \
             provide a concise 'errorMessage' explaining what's missing.
           - If you provide an 'errorMessage', 'providerInput' should be null.

        OUTPUT:
        Return ONLY valid JSON.
        IMPORTANT: All string values MUST have newlines and special characters properly escaped.

        {
          "providerInput": { ... },
          "errorMessage": null or "..."
        }
        """;
  }

  @Override
  protected String additionalPlanContext(String owner, String modelId,
      String description, String bestPractices) {
    var modelDocs = description + "\n\nBest Practices:\n" + bestPractices;
    return "Model Docs: " + modelDocs;
  }

  @Override
  public JsonNode execute(String owner, String modelId, JsonNode input) {
    // fal.ai uses owner/modelId as the model path (e.g., "fal-ai/flux/schnell")
    var modelPath = owner + "/" + modelId;
    return falClient.submit(modelPath, input);
  }

  @Override
  public String extractOutputUrl(JsonNode output) {
    // fal.ai image models: {"images": [{"url": "...", ...}]}
    if (output.has("images") && output.get("images").isArray()
        && !output.get("images").isEmpty()) {
      return output.get("images").get(0).path("url").asText();
    }
    // fal.ai video models: {"video": {"url": "..."}}
    if (output.has("video") && output.get("video").has("url")) {
      return output.get("video").path("url").asText();
    }
    // fal.ai audio models: {"audio": {"url": "..."}}
    if (output.has("audio") && output.get("audio").has("url")) {
      return output.get("audio").path("url").asText();
    }
    // Generic fallback: look for top-level url
    if (output.has("url")) {
      return output.get("url").asText();
    }
    return output.toString();
  }
}
