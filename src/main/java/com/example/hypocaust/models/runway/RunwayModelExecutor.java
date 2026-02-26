package com.example.hypocaust.models.runway;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.runway.api-key")
@Slf4j
public class RunwayModelExecutor extends AbstractModelExecutor {

  private final RunwayClient runwayClient;

  public RunwayModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      RunwayClient runwayClient) {
    super(modelRegistry, objectMapper);
    this.runwayClient = runwayClient;
  }

  @Override
  public Platform platform() {
    return Platform.RUNWAY;
  }

  @Override
  protected String planSystemPrompt() {
    return """
        YOUR RESPONSIBILITIES:
        1. Input Mapping: Construct the 'providerInput' object for the Runway API:
           - Text-to-video models ('gen4-turbo'): requires 'promptText' describing the scene with
             cinematographic detail (subject, action, camera move, lighting, mood). Optionally
             'duration' (5 or 10 seconds), 'ratio' (e.g., "1280:720", "720:1280", "1104:832").
           - Image-to-video models ('gen4-turbo-i2v'): requires 'promptImage' ('@artifact_name'
             placeholder for the source image URL) and 'promptText' describing ONLY the desired
             motion and camera behavior.
           - Upscale ('upscale-v1'): requires 'inputImage' ('@artifact_name' placeholder).
           - Optimize prompts for cinematic, filmmaker-quality output using shot description
             vocabulary (lens, camera move, lighting setup, mood, color grade).
           - If a field requires an image/video and the user refers to an artifact, use
             '@artifact_name' as a placeholder.
        2. Validation:
           - Text-to-video requires a prompt. Image-to-video requires both an image artifact and
             a motion prompt. Flag missing mandatory inputs with 'errorMessage'.
           - If you provide an 'errorMessage', 'providerInput' should be null.
        
        OUTPUT: Return ONLY valid JSON.
        {
          "providerInput": { "promptText": "...", "duration": 10, "ratio": "1280:720" },
          "errorMessage": null or "..."
        }
        """;
  }

  @Override
  protected String additionalPlanContext(String owner, String modelId,
      String description, String bestPractices) {
    return "Model Docs: " + description + "\n\nBest Practices:\n" + bestPractices;
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    return switch (modelId) {
      case "gen4-turbo" -> runwayClient.generateVideo(modelId, input);
      case "gen4-turbo-i2v" -> runwayClient.generateVideoFromImage(modelId, input);
      case "upscale-v1" -> runwayClient.upscale(input);
      default -> {
        log.warn("Unknown Runway model ID: {}, attempting generic video generation", modelId);
        yield runwayClient.generateVideo(modelId, input);
      }
    };
  }

  @Override
  public String extractOutput(JsonNode output) {
    // Runway tasks are async; client polls and resolves to a final output object
    // Expected resolved convention: {"url": "https://...", "status": "SUCCEEDED"}
    if (output.has("url")) {
      return output.get("url").asText();
    }
    // Check Gen-3 artifacts array
    if (output.has("artifacts") && output.get("artifacts").isArray()
        && !output.get("artifacts").isEmpty()) {
      JsonNode first = output.get("artifacts").get(0);
      if (first.has("url")) {
        return first.get("url").asText();
      }
    }
    // Intermediate: task ID returned while polling
    if (output.has("id")) {
      return output.get("id").asText();
    }
    // Nested output array: {"output": ["https://..."]}
    if (output.has("output") && output.get("output").isArray()
        && !output.get("output").isEmpty()) {
      return output.get("output").get(0).asText();
    }
    return output.toString();
  }
}
