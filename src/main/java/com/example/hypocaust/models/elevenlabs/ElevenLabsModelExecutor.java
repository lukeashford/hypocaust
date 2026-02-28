package com.example.hypocaust.models.elevenlabs;

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
@ConditionalOnProperty(name = "app.elevenlabs.api-key")
@Slf4j
public class ElevenLabsModelExecutor extends AbstractModelExecutor {

  private final ElevenLabsClient elevenLabsClient;

  public ElevenLabsModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, ElevenLabsClient elevenLabsClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate);
    this.elevenLabsClient = elevenLabsClient;
  }

  @Override
  public Platform platform() {
    return Platform.ELEVENLABS;
  }

  @Override
  public ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("elevenlabs-plan", """
            You are an expert creative director. Prepare an ElevenLabs generation plan.
            
            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object matching ElevenLabs:
               - 'v3' (TTS): requires 'text' (the script), optionally 'voice_id'.
               - 'voice-design': requires 'voice_description', 'text' (preview script).
               - 'dubbing': requires 'source_url' ('@artifact_name'), 'target_lang'.
               - 'sound-generation': requires 'text' (sound description).
               - If a field requires a URL and the user refers to an artifact, use '@artifact_name' as a placeholder.
            2. Validation:
               - If mandatory info is missing, provide an 'errorMessage'.
            
            OUTPUT: Return ONLY valid JSON:
            {
              "providerInput": { "text": "...", "voice_id": "...", ... },
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
  public String extractOutput(JsonNode output) {
    // ElevenLabs returns audio as binary; client should upload and return a URL
    // Expected client convention: {"url": "https://..."}
    if (output.has("url")) {
      return output.get("url").asText();
    }

    // Voice design returns an array of previews
    if (output.isArray() && !output.isEmpty()) {
      JsonNode first = output.get(0);
      if (first.has("generated_voice_id")) {
        return first.get("generated_voice_id").asText();
      }
    }

    // Dubbing finished
    if (output.has("status") && "finished".equalsIgnoreCase(output.get("status").asText())) {
      JsonNode targets = output.path("target_languages");
      if (targets.isArray() && !targets.isEmpty()) {
        return targets.get(0).path("dubbed_file_url").asText();
      }
    }

    // Dubbing returns a dubbing_id for async polling; surface the id as the result
    if (output.has("dubbing_id")) {
      return output.get("dubbing_id").asText();
    }
    // Voice design returns voice_id
    if (output.has("voice_id")) {
      return output.get("voice_id").asText();
    }
    return output.toString();
  }
}
