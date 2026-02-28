package com.example.hypocaust.models.runway;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.runway.api-key")
@Slf4j
public class RunwayModelExecutor extends AbstractModelExecutor {

  private final RunwayClient runwayClient;

  public RunwayModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, RunwayClient runwayClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate);
    this.runwayClient = runwayClient;
  }

  @Override
  public Platform platform() {
    return Platform.RUNWAY;
  }

  @Override
  public ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("runway-plan", """
            You are an expert creative director. Prepare a Runway Gen-4 generation plan.
            
            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object for the Runway API:
               - Text-to-video models ('gen4-turbo'): requires 'promptText' describing the scene with cinematographic detail.
               - Image-to-video models ('gen4-turbo-i2v'): requires 'promptImage' ('@artifact_name' placeholder) and 'promptText' describing ONLY the desired motion.
               - Upscale ('upscale-v1'): requires 'inputImage' ('@artifact_name' placeholder).
               - Optimize prompts for cinematic quality (lens, camera move, lighting, mood).
               - If a field requires an image/video and the user refers to an artifact, use '@artifact_name' as a placeholder.
            2. Validation:
               - If mandatory info is missing, provide an 'errorMessage'.
            
            OUTPUT: Return ONLY valid JSON:
            {
              "providerInput": { "promptText": "...", "duration": 10, "ratio": "1280:720" },
              "errorMessage": null or "..."
            }
            """))
        .with(PromptFragments.abilityAwareness())
        .build();

    var userPrompt = String.format("""
        Task: %s
        Kind: %s
        Model Docs: %s
        
        Best Practices:
        %s
        """, task, kind, description, bestPractices);

    var response = chatService.call(PROMPT_ENG_MODEL, systemPrompt, userPrompt);
    try {
      var node = objectMapper.readTree(
          com.example.hypocaust.common.JsonUtils.extractJson(response));
      return new ExecutionPlan(
          node.path("providerInput"),
          node.path("errorMessage").isTextual() ? node.path("errorMessage").asText() : null
      );
    } catch (Exception e) {
      return ExecutionPlan.error("Plan generation failed: " + e.getMessage());
    }
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
