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
import java.util.ArrayList;
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
      - eleven_ttv_v3 (voice design): use "preview_0" as the output key (single preview).
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
   * Searches both own voices and the shared voice library for a voice matching the description.
   *
   * <ol>
   *   <li>Asks Haiku to generate 3 structured search queries (with filters: gender, age, accent,
   *       language, search) in a single call.</li>
   *   <li>Runs each query against both own voices ({@code /v2/voices}) and the shared library
   *       ({@code /v1/shared-voices}), collecting unique candidates.</li>
   *   <li>Presents all candidates to Haiku to pick the best match, regardless of source.</li>
   *   <li>If the chosen voice is from the shared library, adds it to the account first.</li>
   * </ol>
   */
  private Optional<JsonNode> findMatchingVoice(String voiceDescription) {
    // Step 1: Generate 3 structured search queries in a single Haiku call
    String queriesSystem = """
        You are helping search a voice library. Generate exactly 3 different search queries
        as JSON objects to find voices matching the given voice description.

        Each query object can have these fields (all optional strings):
        - "gender": e.g. "male", "female"
        - "age": e.g. "young", "middle_aged", "old"
        - "accent": e.g. "british", "american", "australian"
        - "language": ISO code e.g. "en", "de", "fr", "es"
        - "search": free-text search keywords (2-4 words)

        IMPORTANT RULES:
        - Always fill ALL five fields for each query. If the voice description doesn't specify
          a field, choose a plausible random value that fits the overall character. For example,
          for "deep German bass", gender should be "male" and language "de", but age could be
          anything suitable — just pick one.
        - Vary the queries to increase diversity of results. Use different combinations of
          accent, age, and search terms across the 3 queries.
        - The "search" field should contain descriptive keywords about vocal quality, tone, or
          use-case — NOT just repeat the other filter values.

        Return ONLY a JSON array of 3 objects. No explanation.
        Example for "warm British bass narrator":
        [
          {"gender":"male","age":"middle_aged","accent":"british","language":"en","search":"warm deep narrator"},
          {"gender":"male","age":"old","accent":"british","language":"en","search":"bass storytelling voice"},
          {"gender":"male","age":"middle_aged","accent":"british","language":"en","search":"rich authoritative baritone"}
        ]
        """;
    String queriesResponse;
    try {
      queriesResponse = chatService.call(PROMPT_ENG_MODEL, queriesSystem,
          "Voice description: " + voiceDescription, String.class);
    } catch (Exception e) {
      log.warn("[ElevenLabs] Query generation failed, skipping library search: {}",
          e.getMessage());
      return Optional.empty();
    }

    List<JsonNode> queries = parseSearchQueries(queriesResponse);
    log.info("[ElevenLabs] Voice search | desc='{}' | {} queries generated",
        voiceDescription, queries.size());

    // Step 2: Run each query against both own voices and shared library
    Map<String, JsonNode> candidateMap = new LinkedHashMap<>();
    for (int i = 0; i < queries.size(); i++) {
      JsonNode query = queries.get(i);
      log.info("[ElevenLabs] Voice search query[{}]: {}", i, query);

      // Search shared library with structured filters
      try {
        List<JsonNode> shared = elevenLabsClient.searchSharedVoices(query);
        for (JsonNode voice : shared) {
          candidateMap.putIfAbsent(voice.path("voice_id").asText(), voice);
        }
      } catch (Exception e) {
        log.warn("[ElevenLabs] Shared voice search failed for query[{}]: {}", i, e.getMessage());
      }

      // Search own voices with the "search" text
      String searchText = query.path("search").asText("");
      if (!searchText.isBlank()) {
        try {
          List<JsonNode> own = elevenLabsClient.searchOwnVoices(searchText, 3);
          for (JsonNode voice : own) {
            candidateMap.putIfAbsent(voice.path("voice_id").asText(), voice);
          }
        } catch (Exception e) {
          log.warn("[ElevenLabs] Own voice search failed for query[{}]: {}", i, e.getMessage());
        }
      }
    }

    log.info("[ElevenLabs] Voice search → {} unique candidate(s) found", candidateMap.size());

    if (candidateMap.isEmpty()) {
      log.info("[ElevenLabs] No candidates from voice search, will design a new voice");
      return Optional.empty();
    }

    // Step 3: Ask Haiku to pick the best match from all candidates
    StringBuilder voiceList = new StringBuilder();
    for (JsonNode voice : candidateMap.values()) {
      voiceList.append("- voice_id: ").append(voice.path("voice_id").asText())
          .append(", name: \"").append(voice.path("name").asText()).append("\"")
          .append(", description: \"").append(voice.path("description").asText()).append("\"");
      JsonNode labels = voice.path("labels");
      if (!labels.isMissingNode() && labels.isObject() && labels.size() > 0) {
        voiceList.append(", labels: ").append(labels);
      }
      voiceList.append(" [").append(voice.path("source").asText("unknown")).append("]\n");
    }

    String selectionSystem = """
        You are a voice casting assistant. Given a required voice description and a list of
        candidate voices, pick the best matching voice_id. Be selective — only pick a voice if
        it genuinely fits the description. Do not prefer any source (own vs shared) — just pick
        the best fit regardless. If none is a good match, return the word null.
        Respond with ONLY the voice_id string or the word null. No explanation.
        """;
    String selectionUser =
        "Required voice: " + voiceDescription + "\n\nCandidates:\n" + voiceList;

    String response = chatService.call(PROMPT_ENG_MODEL, selectionSystem, selectionUser,
        String.class);
    if (response == null || response.isBlank() || "null".equalsIgnoreCase(response.strip())) {
      log.info("[ElevenLabs] Voice match: Haiku found no suitable voice, will design a new one");
      return Optional.empty();
    }

    String matchedId = response.strip();
    Optional<JsonNode> matched = candidateMap.values().stream()
        .filter(v -> matchedId.equals(v.path("voice_id").asText()))
        .findFirst();

    if (matched.isEmpty()) {
      log.warn("[ElevenLabs] Voice match: Haiku returned unknown voice_id='{}', ignoring",
          matchedId);
      return Optional.empty();
    }

    JsonNode matchedVoice = matched.get();
    log.info("[ElevenLabs] Voice match: voice_id='{}' name='{}' source='{}'",
        matchedId, matchedVoice.path("name").asText(), matchedVoice.path("source").asText());

    // Step 4: If from shared library, add it to our account first
    if ("shared".equals(matchedVoice.path("source").asText())) {
      String publicOwnerId = matchedVoice.path("public_owner_id").asText("");
      if (publicOwnerId.isBlank()) {
        log.warn("[ElevenLabs] Shared voice '{}' has no public_owner_id, cannot add to account",
            matchedId);
        return Optional.empty();
      }
      try {
        String voiceName = matchedVoice.path("name").asText("Shared Voice");
        JsonNode added = elevenLabsClient.addSharedVoice(publicOwnerId, matchedId, voiceName);
        String addedVoiceId = added.path("voice_id").asText();
        if (!addedVoiceId.isBlank()) {
          log.info("[ElevenLabs] Shared voice added to account: '{}' → '{}'",
              matchedId, addedVoiceId);
          // Update the voice_id to the newly added one (may differ from shared ID)
          ((ObjectNode) matchedVoice).put("voice_id", addedVoiceId);
        }
      } catch (Exception e) {
        log.warn("[ElevenLabs] Failed to add shared voice '{}' to account: {}",
            matchedId, e.getMessage());
        return Optional.empty();
      }
    }

    return matched;
  }

  /**
   * Parses the structured search queries returned by Haiku. Expects a JSON array of objects with
   * filter fields (gender, age, accent, language, search).
   */
  private List<JsonNode> parseSearchQueries(String response) {
    if (response == null || response.isBlank()) {
      return List.of();
    }
    try {
      JsonNode node = objectMapper.readTree(
          com.example.hypocaust.common.JsonUtils.extractJson(response));
      if (node.isArray()) {
        List<JsonNode> queries = new ArrayList<>();
        for (JsonNode item : node) {
          if (item.isObject()) {
            queries.add(item);
          }
        }
        return queries.isEmpty() ? List.of() : queries;
      }
    } catch (Exception e) {
      log.warn("[ElevenLabs] Could not parse search query JSON: {}", e.getMessage());
    }
    return List.of();
  }

  /**
   * Build TTS phases.
   *
   * <ul>
   *   <li>If {@code voice_id} is present: single-phase direct TTS.</li>
   *   <li>Otherwise: delegate to {@link #buildVoiceDesignPhases} (always 1 phase) + TTS = 2
   *       phases total.</li>
   * </ul>
   */
  private List<ExecutionPhase> buildTtsPhases(JsonNode input) {
    boolean hasVoiceId = input.has("voice_id")
        && !input.get("voice_id").asText().isBlank();

    if (hasVoiceId) {
      log.info("[ElevenLabs] TTS strategy: direct (voice_id='{}' present)",
          input.get("voice_id").asText());
      return List.of((orig, prev) -> elevenLabsClient.textToSpeech(orig));
    }

    // No voice_id: resolve voice via voice design (which searches the library first)
    log.info("[ElevenLabs] TTS: no voice_id — delegating voice resolution to voice design");
    List<ExecutionPhase> phases = new ArrayList<>(buildVoiceDesignPhases(input));

    // Append TTS phase that picks up the voiceId from the first voice design preview
    phases.add((orig, prev) -> {
      var previews = prev.path("previews");
      if (!previews.isArray() || previews.isEmpty()) {
        throw new IllegalStateException(
            "Voice design produced no previews to use for TTS");
      }
      String voiceId = previews.get(0).path("voiceId").asText();
      if (voiceId.isBlank()) {
        throw new IllegalStateException(
            "Voice design preview is missing voiceId; preview=" + previews.get(0));
      }
      log.info("[ElevenLabs] TTS: using voice_id='{}' from voice design | text_len={}",
          voiceId, orig.path("text").asText().length());
      ObjectNode ttsInput = objectMapper.createObjectNode();
      ttsInput.put("text", orig.get("text").asText());
      ttsInput.put("voice_id", voiceId);
      var ttsResult = elevenLabsClient.textToSpeech(ttsInput);
      if (ttsResult.isObject()) {
        ((ObjectNode) ttsResult).put("voiceId", voiceId);
      }
      return ttsResult;
    });

    return phases;
  }

  /**
   * Build voice design phases. If fresh_voice is set, generate a new voice directly. Otherwise,
   * search both own voices and the shared library first; if a match is found return it as a single
   * preview, else fall back to generating a new voice. Always a single phase.
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

    // Single phase: design + save the first preview
    return List.of((orig, prev) -> {
      log.info("[ElevenLabs] Voice Design: design + save first preview | desc='{}'",
          orig.path("voice_description").asText());
      JsonNode designResult = elevenLabsClient.voiceDesign(orig);

      var previews = designResult.path("previews");
      if (!previews.isArray() || previews.isEmpty()) {
        throw new IllegalStateException("Voice design returned no previews");
      }

      // Save only the first preview — the other 2 are discarded
      var firstPreview = previews.get(0);
      String genId = firstPreview.path("generated_voice_id").asText();
      if (genId.isBlank()) {
        throw new IllegalStateException(
            "Voice design preview[0] missing generated_voice_id; entry=" + firstPreview);
      }

      String voiceDesc = orig.path("voice_description").asText("designed voice");
      String voiceName = voiceDesc.length() > 100 ? voiceDesc.substring(0, 97) + "..." : voiceDesc;
      var saved = elevenLabsClient.saveVoice(genId, voiceName, voiceDesc);

      log.info("[ElevenLabs] Voice Design: saved first preview generated_voice_id='{}' → voice_id='{}'",
          genId, saved.path("voice_id").asText());

      var result = objectMapper.createObjectNode();
      var savedPreviews = result.putArray("previews");
      var entry = savedPreviews.addObject();
      entry.put("url", firstPreview.path("url").asText());
      entry.put("voiceId", saved.path("voice_id").asText());
      return result;
    });
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
