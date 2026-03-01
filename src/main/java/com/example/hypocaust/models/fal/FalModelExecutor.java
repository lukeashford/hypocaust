package com.example.hypocaust.models.fal;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.fal.api-key")
@Slf4j
public class FalModelExecutor extends AbstractModelExecutor {

  private final FalClient falClient;

  public FalModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      FalClient falClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService);
    this.falClient = falClient;
  }

  @Override
  public Platform platform() {
    return Platform.FAL;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("fal-plan", """
            You are an expert creative director. Prepare a fal.ai generation plan.

            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object matching the fal.ai model's expected input format.
               - Optimize prompts for the best artistic results.
               - If a field requires a URL/image and the user refers to an artifact, use '@artifact_name' as a placeholder.
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
    var modelPath = owner + "/" + modelId;
    return falClient.submit(modelPath, input);
  }

  @Override
  protected String extractOutput(JsonNode output) {
    if (output.has("images") && output.get("images").isArray()
        && !output.get("images").isEmpty()) {
      return output.get("images").get(0).path("url").asText();
    }
    if (output.has("video") && output.get("video").has("url")) {
      return output.get("video").path("url").asText();
    }
    if (output.has("audio") && output.get("audio").has("url")) {
      return output.get("audio").path("url").asText();
    }
    if (output.has("url")) {
      return output.get("url").asText();
    }
    return output.toString();
  }
}
