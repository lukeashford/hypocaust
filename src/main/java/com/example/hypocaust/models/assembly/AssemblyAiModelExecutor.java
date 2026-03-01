package com.example.hypocaust.models.assembly;

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
@ConditionalOnProperty(name = "app.assemblyai.api-key")
@Slf4j
public class AssemblyAiModelExecutor extends AbstractModelExecutor {

  private final AssemblyAiClient assemblyAiClient;

  public AssemblyAiModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      AssemblyAiClient assemblyAiClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService);
    this.assemblyAiClient = assemblyAiClient;
  }

  @Override
  public Platform platform() {
    return Platform.ASSEMBLYAI;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("assemblyai-plan", """
            You are an expert creative director. Prepare an AssemblyAI processing plan.

            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object following the model's input
               spec described in the Model Docs and Best Practices below.
               - If a field requires an audio URL and the user refers to an artifact, use '@artifact_name'.
            2. Validation:
               - If mandatory audio source is missing, provide an 'errorMessage'.

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
    return switch (modelId) {
      case "transcript" -> assemblyAiClient.transcribe(input);
      case "audio-intelligence" -> assemblyAiClient.transcribeWithIntelligence(input);
      default -> {
        log.warn("Unknown AssemblyAI model ID: {}, falling back to transcription", modelId);
        yield assemblyAiClient.transcribe(input);
      }
    };
  }

  @Override
  protected String extractOutput(JsonNode output) {
    if (output.has("text") && output.get("text").isTextual()) {
      return output.get("text").asText();
    }
    if (output.has("id")) {
      return output.get("id").asText();
    }
    if (output.has("chapters")) {
      return output.toString();
    }
    return output.toString();
  }
}
