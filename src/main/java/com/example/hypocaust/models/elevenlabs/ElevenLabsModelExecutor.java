package com.example.hypocaust.models.elevenlabs;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.elevenlabs.api-key")
@Slf4j
public class ElevenLabsModelExecutor extends AbstractModelExecutor {

  private final ElevenLabsClient elevenLabsClient;

  public ElevenLabsModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ElevenLabsClient elevenLabsClient) {
    super(modelRegistry, objectMapper);
    this.elevenLabsClient = elevenLabsClient;
  }

  @Override
  public Platform platform() {
    return Platform.ELEVENLABS;
  }

  @Override
  protected String planSystemPrompt() {
    return """
        YOUR RESPONSIBILITIES:
        1. Input Mapping: Construct the 'providerInput' object matching the ElevenLabs endpoint's
           expected input format based on the model ID:
           - 'v3' (TTS): requires 'text' (the script), optionally 'voice_id', 'stability',
             'similarity_boost', 'style', 'use_speaker_boost'.
           - 'voice-design': requires 'voice_description' (detailed vocal characteristics),
             'text' (sample text to preview), optionally 'loudness', 'quality', 'age'.
           - 'dubbing': requires 'source_url' or '@artifact_name' reference to source video/audio,
             'target_lang', optionally 'num_speakers', 'watermark'.
           - 'sound-generation': requires 'text' (sound description), optionally 'duration_seconds',
             'prompt_influence'.
           - Optimize prompts for natural, expressive audio output.
           - If a field requires a URL/audio and the user refers to an artifact, use '@artifact_name'
             as a placeholder.
        2. Validation:
           - If the user task is missing information that is MANDATORY (e.g., no script text for TTS,
             no target language for dubbing), provide a concise 'errorMessage'.
           - If you provide an 'errorMessage', 'providerInput' should be null.
        
        OUTPUT: Return ONLY valid JSON.
        {
          "providerInput": { "text": "...", "voice_id": "...", ... },
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
  public JsonNode execute(String owner, String modelId, JsonNode input) {
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
  public String extractOutputUrl(JsonNode output) {
    // ElevenLabs returns audio as binary; client should upload and return a URL
    // Expected client convention: {"url": "https://..."}
    if (output.has("url")) {
      return output.get("url").asText();
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
