package com.example.hypocaust.models.runway;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      RunwayClient runwayClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService);
    this.runwayClient = runwayClient;
  }

  @Override
  public Platform platform() {
    return Platform.RUNWAY;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("runway-plan", """
            You are an expert creative director. Prepare a Runway generation plan.
            
            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object following the model's input
               spec described in the Model Docs and Best Practices below.
               - Optimize prompts for cinematic quality (lens, camera move, lighting, mood).
               - If a field requires an image/video and the user refers to an artifact, use '@artifact_name' as a placeholder.
            2. Validation:
               - If mandatory info is missing, provide an 'errorMessage'.
            
            OUTPUT: Return ONLY valid JSON:
            {
              "providerInput": { ... },
              "errorMessage": null or "..."
            }
            """))
        .with(PromptFragments.abilityAwareness())
        .build();

    var userPrompt = String.format("""
        Task: %s
        Model Docs: %s
        
        Best Practices:
        %s
        """, task, model.description(), model.bestPractices());

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
  protected List<String> extractOutputs(JsonNode output) {
    if (output.has("url")) {
      return List.of(output.get("url").asText());
    }
    if (output.has("artifacts") && output.get("artifacts").isArray()
        && !output.get("artifacts").isEmpty()) {
      JsonNode first = output.get("artifacts").get(0);
      if (first.has("url")) {
        return List.of(first.get("url").asText());
      }
    }
    if (output.has("id")) {
      return List.of(output.get("id").asText());
    }
    if (output.has("output") && output.get("output").isArray()
        && !output.get("output").isEmpty()) {
      return List.of(output.get("output").get(0).asText());
    }
    return List.of(output.toString());
  }
}
