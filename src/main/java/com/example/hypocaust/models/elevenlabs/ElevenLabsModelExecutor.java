package com.example.hypocaust.models.elevenlabs;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static final String ELEVENLABS_SYSTEM_PROMPT = """
      You are planning for an ElevenLabs model.

      PLATFORM CONSTRAINTS (ElevenLabs enforced — violations cause API rejections):
      - If you provide a 'text' field for Voice Design (eleven_ttv_v3), it MUST be at least 100 characters long.
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

      VOICE ID:
      - To reference a voice from an existing artifact, use '@artifact_name.metadata.voiceId'
        as the voice_id value. Do NOT invent or guess voice IDs.
      - If no voice_id is available but a voice is described, omit 'voice_id' entirely and
        include 'voice_description' with the description and 'text' with the speech content.
        The system will automatically search the voice library for a matching voice before
        falling back to generating a fresh one.
      - Set 'fresh_voice' to true ONLY when the task explicitly asks to generate or create a
        new voice (e.g., "generate a fresh voice", "create a new voice for this character").
        Omit 'fresh_voice' or set it to false in all other cases.

      OUTPUT KEY CONVENTIONS for outputMapping:
      - eleven_v3 / sound-generation / dubbing: use "audio" as the output key.
      - eleven_ttv_v3 (voice design previews): use "preview_0", "preview_1", etc. as output keys.
      """;

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts) {
    return generatePlanWithLlm(task, model, artifacts, ELEVENLABS_SYSTEM_PROMPT, null);
  }

  @Override
  protected List<ExecutionPhase> buildExecutionPhases(String owner, String modelId,
      JsonNode input) {
    return switch (modelId) {
      case "eleven_v3" -> buildTtsPhases(input);
      case "eleven_ttv_v3" -> buildVoiceDesignPhases(input);
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
   * Searches the ElevenLabs voice library for a voice matching the given description. Fetches all
   * available voices (own + premade), then asks Haiku to pick the best match. Returns the matched
   * voice node (containing {@code voice_id} and {@code preview_url}), or empty if none fits.
   */
  private Optional<JsonNode> findMatchingVoice(String voiceDescription) {
    List<JsonNode> voices;
    try {
      voices = elevenLabsClient.listAllVoices();
    } catch (Exception e) {
      log.warn("[ElevenLabs] Library lookup failed, will generate new voice: {}", e.getMessage());
      return Optional.empty();
    }

    if (voices == null || voices.isEmpty()) {
      return Optional.empty();
    }

    StringBuilder voiceList = new StringBuilder();
    for (JsonNode voice : voices) {
      voiceList.append("- voice_id: ").append(voice.path("voice_id").asText())
          .append(", name: \"").append(voice.path("name").asText()).append("\"")
          .append(", description: \"").append(voice.path("description").asText()).append("\"");
      JsonNode labels = voice.path("labels");
      if (!labels.isMissingNode() && labels.isObject() && labels.size() > 0) {
        voiceList.append(", labels: ").append(labels);
      }
      voiceList.append("\n");
    }

    String system = """
        You are a voice casting assistant. Given a required voice description and a list of
        available voices, pick the best matching voice_id. Be selective — only pick a voice if
        it genuinely fits the description. If no voice is a good match, return the word null.
        Respond with ONLY the voice_id string or the word null. No explanation.
        """;
    String user = "Required voice: " + voiceDescription + "\n\nAvailable voices:\n" + voiceList;

    String response = chatService.call(PROMPT_ENG_MODEL, system, user);
    if (response == null || response.isBlank() || "null".equalsIgnoreCase(response.strip())) {
      log.info("[ElevenLabs] Library match: no suitable voice found for description");
      return Optional.empty();
    }

    String matchedId = response.strip();
    Optional<JsonNode> matched = voices.stream()
        .filter(v -> matchedId.equals(v.path("voice_id").asText()))
        .findFirst();

    if (matched.isPresent()) {
      log.info("[ElevenLabs] Library match: voice_id='{}' name='{}'",
          matchedId, matched.get().path("name").asText());
    } else {
      log.warn("[ElevenLabs] Library match: Haiku returned unknown voice_id='{}', ignoring",
          matchedId);
    }
    return matched;
  }

  /**
   * Build TTS phases. If voice_id is present, single-phase direct TTS. If fresh_voice is set,
   * skip library search and chain: design → save → TTS. Otherwise, search the voice library first;
   * if a match is found use it directly, else fall back to the design → save → TTS chain.
   */
  private List<ExecutionPhase> buildTtsPhases(JsonNode input) {
    boolean hasVoiceId = input.has("voice_id")
        && !input.get("voice_id").asText().isBlank();

    if (hasVoiceId) {
      log.info("[ElevenLabs] TTS strategy: direct (voice_id='{}' present)",
          input.get("voice_id").asText());
      return List.of((orig, prev) -> elevenLabsClient.textToSpeech(orig));
    }

    if (!input.path("fresh_voice").asBoolean(false)) {
      String description = input.path("voice_description").asText();
      Optional<JsonNode> match = findMatchingVoice(description);
      if (match.isPresent()) {
        String matchedVoiceId = match.get().path("voice_id").asText();
        log.info("[ElevenLabs] TTS strategy: library match (voice_id='{}')", matchedVoiceId);
        return List.of((orig, prev) -> {
          ObjectNode ttsInput = orig.deepCopy();
          ttsInput.put("voice_id", matchedVoiceId);
          var result = elevenLabsClient.textToSpeech(ttsInput);
          if (result.isObject()) {
            ((ObjectNode) result).put("voiceId", matchedVoiceId);
          }
          return result;
        });
      }
    }

    log.info("[ElevenLabs] TTS strategy: chain ({}) → voice design → save → TTS",
        input.path("fresh_voice").asBoolean(false) ? "fresh_voice=true" : "no library match");
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
   * Build voice design phases. If fresh_voice is set, generate a new voice (design → save all
   * previews). Otherwise, search the voice library first; if a match is found return it as a
   * single preview, else fall back to generating a new voice.
   */
  private List<ExecutionPhase> buildVoiceDesignPhases(JsonNode input) {
    if (!input.path("fresh_voice").asBoolean(false)) {
      String description = input.path("voice_description").asText();
      Optional<JsonNode> match = findMatchingVoice(description);
      if (match.isPresent()) {
        JsonNode matchedVoice = match.get();
        String matchedVoiceId = matchedVoice.path("voice_id").asText();
        String previewUrl = matchedVoice.path("preview_url").asText();
        log.info("[ElevenLabs] Voice Design strategy: library match (voice_id='{}')",
            matchedVoiceId);
        return List.of((orig, prev) -> {
          ObjectNode result = objectMapper.createObjectNode();
          var previews = result.putArray("previews");
          var entry = previews.addObject();
          entry.put("url", previewUrl);
          entry.put("voiceId", matchedVoiceId);
          return result;
        });
      }
    }

    log.info("[ElevenLabs] Voice Design strategy: generate new voice ({})",
        input.path("fresh_voice").asBoolean(false) ? "fresh_voice=true" : "no library match");
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
      case "eleven_v3" -> elevenLabsClient.textToSpeech(input);
      case "eleven_ttv_v3" -> elevenLabsClient.voiceDesign(input);
      case "dubbing" -> elevenLabsClient.dubbing(input);
      case "sound-generation" -> elevenLabsClient.soundGeneration(input);
      default -> {
        log.warn("Unknown ElevenLabs model ID: {}", modelId);
        yield elevenLabsClient.textToSpeech(input);
      }
    };
  }

  @Override
  protected Map<String, ExtractedOutput> extractOutputs(JsonNode output) {
    Map<String, ExtractedOutput> results = new LinkedHashMap<>();

    // Voice design previews: array of {url, voiceId}
    if (output.has("previews") && output.get("previews").isArray()) {
      var previews = output.get("previews");
      for (int i = 0; i < previews.size(); i++) {
        var preview = previews.get(i);
        ObjectNode meta = null;
        if (preview.has("voiceId")) {
          meta = objectMapper.createObjectNode();
          meta.put("voiceId", preview.get("voiceId").asText());
        }
        results.put("preview_" + i, new ExtractedOutput(preview.get("url").asText(), meta));
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
      results.put("audio", new ExtractedOutput(output.get("url").asText(), meta));
      return results;
    }

    // Dubbing: finished with target languages
    if (output.has("status") && "finished".equalsIgnoreCase(output.get("status").asText())) {
      JsonNode targets = output.path("target_languages");
      if (targets.isArray() && !targets.isEmpty()) {
        results.put("audio", ExtractedOutput.ofContent(
            targets.get(0).path("dubbed_file_url").asText()));
        return results;
      }
    }
    if (output.has("dubbing_id")) {
      results.put("audio", ExtractedOutput.ofContent(output.get("dubbing_id").asText()));
      return results;
    }

    results.put("audio", ExtractedOutput.ofContent(output.toString()));
    return results;
  }
}
