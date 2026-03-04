package com.example.hypocaust.models.elevenlabs;

import com.example.hypocaust.domain.IntentMapping;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
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
      ArtifactResolver artifactResolver, ElevenLabsClient elevenLabsClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService,
        artifactResolver);
    this.elevenLabsClient = elevenLabsClient;
  }

  @Override
  public Platform platform() {
    return Platform.ELEVENLABS;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<IntentMapping> intents) {
    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("elevenlabs-plan", """
            You are an expert creative director. Prepare an ElevenLabs generation plan.
            
            You are planning for model: %s (id: %s)
            
            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' object following the model's input
               spec described in the Model Docs and Best Practices below.
               - If a field requires a URL and the user refers to an artifact, use '@artifact_name' as a placeholder.
               - To reference a voice from an existing artifact, use '@artifact_name.metadata.voiceId'
                 as the voice_id value. Do NOT invent or guess voice IDs.
               - If no voice_id is available but a voice is described, omit 'voice_id' entirely and
                 include 'voice_description' with the description and 'text' with the speech content.
                 The system will handle voice creation automatically.
            2. Validation:
               - If mandatory info is missing, provide an 'errorMessage'.
               - Do NOT invent IDs. If you don't have a specific voice_id, omit the field entirely.
            
            PLATFORM CONSTRAINTS (ElevenLabs enforced — violations cause API rejections):
            - If you provide a 'text' field for Voice Design (voice-design), it MUST be at least 100 characters long.
              If the desired sample text is shorter, either extend it with relevant content or omit
              the 'text' field entirely to allow the system to auto-generate a suitable sample line.
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
        %sModel Docs: %s
        
        Best Practices:
        %s
        """, task, formatIntentContext(intents), model.description(), model.bestPractices());

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
  protected List<ExecutionPhase> buildExecutionPhases(String owner, String modelId,
      JsonNode input) {
    return switch (modelId) {
      case "tts" -> buildTtsPhases(input);
      case "voice-design" -> buildVoiceDesignPhases();
      case "sound-generation" -> List.of(
          (orig, prev) -> elevenLabsClient.soundGeneration(orig));
      case "dubbing" -> List.of(
          (orig, prev) -> elevenLabsClient.dubbing(orig));
      default -> {
        log.warn("Unknown ElevenLabs model ID: {}, falling back to TTS", modelId);
        yield buildTtsPhases(input);
      }
    };
  }

  /**
   * Build TTS phases. If voice_id is present, single-phase direct TTS. If only voice_description is
   * present, chain: design → save → TTS.
   */
  private List<ExecutionPhase> buildTtsPhases(JsonNode input) {
    boolean hasVoiceId = input.has("voice_id")
        && !input.get("voice_id").asText().isBlank();

    if (hasVoiceId) {
      log.info("[ElevenLabs] TTS strategy: direct (voice_id='{}' present)",
          input.get("voice_id").asText());
      return List.of((orig, prev) -> elevenLabsClient.textToSpeech(orig));
    }

    log.info("[ElevenLabs] TTS strategy: chain (no voice_id) → voice design → save → TTS");
    return List.of(
        // Phase 1: Design voices based on description
        (orig, prev) -> {
          log.info("[ElevenLabs] TTS Phase 1/3: voice design | desc='{}'",
              orig.path("voice_description").asText());

          // ElevenLabs requires >= 100 chars for custom design text.
          // If too short, omit to allow auto-generation.
          ObjectNode designInput = orig.deepCopy();
          if (designInput.has("text") && designInput.get("text").asText().length() < 100) {
            log.info("[ElevenLabs] TTS Phase 1/3: text too short (<100) for design, omitting.");
            designInput.remove("text");
          }
          return elevenLabsClient.voiceDesign(designInput);
        },

        // Phase 2: Save the first preview to get a permanent voice_id
        (orig, prev) -> {
          var previews = prev.path("previews");
          if (!previews.isArray() || previews.isEmpty()) {
            throw new IllegalStateException("Voice design returned no previews");
          }
          var firstPreview = previews.get(0);
          String genId = firstPreview.path("generated_voice_id").asText();
          if (genId.isBlank()) {
            throw new IllegalStateException(
                "Voice design preview missing generated_voice_id; entry=" + firstPreview);
          }
          String voiceDesc = orig.path("voice_description").asText("designed voice");
          log.info("[ElevenLabs] TTS Phase 2/3: save voice | generated_voice_id='{}'", genId);

          // ElevenLabs limits voice name to 100 characters.
          String voiceName = voiceDesc;
          if (voiceName.length() > 100) {
            voiceName = voiceName.substring(0, 97) + "...";
          }
          return elevenLabsClient.saveVoice(genId, voiceName, voiceDesc);
        },

        // Phase 3: TTS using the saved permanent voice_id
        (orig, prev) -> {
          String voiceId = prev.path("voice_id").asText();
          if (voiceId.isBlank()) {
            throw new IllegalStateException("saveVoice returned no voice_id; response=" + prev);
          }
          log.info("[ElevenLabs] TTS Phase 3/3: TTS | voice_id='{}' text_len={}",
              voiceId, orig.path("text").asText().length());
          ObjectNode ttsInput = objectMapper.createObjectNode();
          ttsInput.put("text", orig.get("text").asText());
          ttsInput.put("voice_id", voiceId);
          var ttsResult = elevenLabsClient.textToSpeech(ttsInput);
          // Carry the permanent voiceId forward so extractOutputs can include it in metadata
          if (ttsResult.isObject()) {
            ((ObjectNode) ttsResult).put("voiceId", voiceId);
          }
          return ttsResult;
        }
    );
  }

  /**
   * Build voice design phases: design → save all previews. Returns previews with permanent
   * voiceIds.
   */
  private List<ExecutionPhase> buildVoiceDesignPhases() {
    return List.of(
        // Phase 1: Design voices
        (orig, prev) -> {
          log.info("[ElevenLabs] Voice Design Phase 1/2: design | desc='{}'",
              orig.path("voice_description").asText());
          return elevenLabsClient.voiceDesign(orig);
        },

        // Phase 2: Save all previews to get permanent voice_ids
        (orig, prev) -> {
          var previews = prev.path("previews");
          if (!previews.isArray() || previews.isEmpty()) {
            throw new IllegalStateException("Voice design returned no previews");
          }
          log.info("[ElevenLabs] Voice Design Phase 2/2: saving {} preview(s)", previews.size());
          String voiceDesc = orig.path("voice_description").asText("designed voice");

          var result = objectMapper.createObjectNode();
          var savedPreviews = result.putArray("previews");

          for (int i = 0; i < previews.size(); i++) {
            var preview = previews.get(i);
            String genId = preview.path("generated_voice_id").asText();
            if (genId.isBlank()) {
              throw new IllegalStateException(
                  "Voice design preview[" + i + "] missing generated_voice_id; entry=" + preview);
            }
            log.info(
                "[ElevenLabs] Voice Design Phase 2/2: saving preview[{}] generated_voice_id='{}'",
                i, genId);

            // ElevenLabs limits voice name to 100 characters.
            String voiceName = voiceDesc + " " + (i + 1);
            if (voiceName.length() > 100) {
              voiceName = voiceName.substring(0, 97) + "...";
            }
            var saved = elevenLabsClient.saveVoice(genId, voiceName, voiceDesc);

            var entry = savedPreviews.addObject();
            entry.put("url", preview.path("url").asText());
            entry.put("voiceId", saved.path("voice_id").asText());
          }
          return result;
        }
    );
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    // Fallback for single-phase execution (used by default buildExecutionPhases)
    return switch (modelId) {
      case "tts" -> elevenLabsClient.textToSpeech(input);
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
  protected List<ExtractedOutput> extractOutputs(JsonNode output) {
    // Voice design previews: array of {url, voiceId}
    if (output.has("previews") && output.get("previews").isArray()) {
      var results = new ArrayList<ExtractedOutput>();
      for (var preview : output.get("previews")) {
        ObjectNode meta = null;
        if (preview.has("voiceId")) {
          meta = objectMapper.createObjectNode();
          meta.put("voiceId", preview.get("voiceId").asText());
        }
        results.add(new ExtractedOutput(preview.get("url").asText(), meta));
      }
      return results;
    }

    // TTS or sound generation: single {url}, optionally with voiceId
    if (output.has("url")) {
      ObjectNode meta = null;
      if (output.has("voiceId")) {
        meta = objectMapper.createObjectNode();
        meta.put("voiceId", output.get("voiceId").asText());
      }
      return List.of(new ExtractedOutput(output.get("url").asText(), meta));
    }

    // Dubbing: finished with target languages
    if (output.has("status") && "finished".equalsIgnoreCase(output.get("status").asText())) {
      JsonNode targets = output.path("target_languages");
      if (targets.isArray() && !targets.isEmpty()) {
        return List.of(ExtractedOutput.ofContent(
            targets.get(0).path("dubbed_file_url").asText()));
      }
    }
    if (output.has("dubbing_id")) {
      return List.of(ExtractedOutput.ofContent(output.get("dubbing_id").asText()));
    }

    return List.of(ExtractedOutput.ofContent(output.toString()));
  }
}
