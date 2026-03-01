package com.example.hypocaust.models.elevenlabs;

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
@ConditionalOnProperty(name = "app.elevenlabs.api-key")
@Slf4j
public class ElevenLabsModelExecutor extends AbstractModelExecutor {

  private final ElevenLabsClient elevenLabsClient;

  public ElevenLabsModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ElevenLabsClient elevenLabsClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService);
    this.elevenLabsClient = elevenLabsClient;
  }

  @Override
  public Platform platform() {
    return Platform.ELEVENLABS;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("elevenlabs-plan", """
            You are an expert creative director. Prepare an ElevenLabs generation plan.
            
            You are planning for model: %s (id: %s)
            
            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object following the model's input
               spec described in the Model Docs and Best Practices below.
               - If a field requires a URL and the user refers to an artifact, use '@artifact_name' as a placeholder.
            2. Validation:
               - If mandatory info is missing, provide an 'errorMessage'.
               - Do NOT invent IDs. If you don't have a specific voice_id, omit the field entirely.
            
            PLATFORM CONSTRAINTS (ElevenLabs enforced — violations cause API rejections):
            - Do NOT describe voices as children, child-like, young children, or minors. This will be blocked.
            - Do NOT reference real people, celebrities, or public figures by name in voice descriptions.
            - Do NOT reference real artists, actors, or voice actors by name (e.g., "sounds like Morgan Freeman").
            - Do NOT request voices for sexual, violent, or hateful content.
            - Instead, describe vocal qualities abstractly: pitch (high/low), tone (warm/sharp/breathy),
              pace (measured/rapid), texture (smooth/raspy/gravelly), accent, and emotional register
              (cheerful/somber/authoritative).
            - For character voices in children's stories: describe the voice as "high-pitched and energetic"
              or "warm and gentle narrator" rather than "a child's voice" or "sounds like a little girl."
            
            OUTPUT: Return ONLY valid JSON:
            {
              "providerInput": { ... },
              "errorMessage": null or "..."
            }
            """.formatted(model.name(), model.modelId())))
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
      case "v3" -> elevenLabsClient.textToSpeech(input);
      case "voice-design" -> elevenLabsClient.voiceDesign(input);
      case "dubbing" -> elevenLabsClient.dubbing(input);
      case "sound-generation" -> elevenLabsClient.soundGeneration(input);
      default -> {
        log.warn("Unknown ElevenLabs model ID: {}", modelId);
        yield elevenLabsClient.textToSpeech(input);
      }
    };
  }

  @Override
  protected List<String> extractOutputs(JsonNode output) {
    if (output.has("url")) {
      return List.of(output.get("url").asText());
    }
    if (output.has("status") && "finished".equalsIgnoreCase(output.get("status").asText())) {
      JsonNode targets = output.path("target_languages");
      if (targets.isArray() && !targets.isEmpty()) {
        return List.of(targets.get(0).path("dubbed_file_url").asText());
      }
    }
    if (output.has("dubbing_id")) {
      return List.of(output.get("dubbing_id").asText());
    }
    return List.of(output.toString());
  }
}
