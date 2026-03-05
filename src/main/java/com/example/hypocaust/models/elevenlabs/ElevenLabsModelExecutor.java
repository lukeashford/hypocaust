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
   * Searches the ElevenLabs voice library for a voice matching the given description.
   *
   * <ol>
   *   <li>Asks Haiku to generate 3 short keyword queries from the description.</li>
   *   <li>Runs each query against {@code /v2/voices?search=…&page_size=3}, collecting up to 9
   *       unique candidate voices.</li>
   *   <li>Presents the trimmed candidate list to Haiku to pick the best match, or null if none
   *       fits.</li>
   * </ol>
   *
   * Returns the matched voice node (containing {@code voice_id} and {@code preview_url}), or
   * {@link Optional#empty()} if no suitable match was found.
   */
  private Optional<JsonNode> findMatchingVoice(String voiceDescription) {
    // Step 1: Generate 3 keyword search queries from the description
    String keywordsSystem = """
        You are helping search a voice library. Generate exactly 3 short keyword queries
        (2–4 words each) to find voices matching the given description. Queries should target
        different aspects: vocal qualities, tone, accent, gender, use-case, etc.
        Return ONLY a JSON array of 3 strings. No explanation.
        Example: ["warm british baritone", "deep male narrator", "storyteller authoritative"]
        """;
    String keywordsResponse;
    try {
      keywordsResponse = chatService.call(PROMPT_ENG_MODEL, keywordsSystem,
          "Voice description: " + voiceDescription);
    } catch (Exception e) {
      log.warn("[ElevenLabs] Keyword generation failed, skipping library search: {}",
          e.getMessage());
      return Optional.empty();
    }

    List<String> queries = parseKeywordQueries(keywordsResponse);
    log.info("[ElevenLabs] Voice library search | desc='{}' | queries={}", voiceDescription,
        queries);

    // Step 2: Run 3 searches, collect first 3 results each (up to 9 unique candidates)
    Map<String, JsonNode> candidateMap = new LinkedHashMap<>();
    for (String query : queries) {
      try {
        List<JsonNode> results = elevenLabsClient.searchVoices(query, 3);
        for (JsonNode voice : results) {
          String voiceId = voice.path("voice_id").asText();
          if (!voiceId.isBlank()) {
            candidateMap.putIfAbsent(voiceId, voice);
          }
        }
      } catch (Exception e) {
        log.warn("[ElevenLabs] Search failed for query='{}': {}", query, e.getMessage());
      }
    }

    log.info("[ElevenLabs] Voice library search → {} unique candidate(s) found",
        candidateMap.size());

    if (candidateMap.isEmpty()) {
      log.info("[ElevenLabs] No candidates from library search, will design a new voice");
      return Optional.empty();
    }

    // Step 3: Ask Haiku to pick the best match from the candidate list
    StringBuilder voiceList = new StringBuilder();
    for (JsonNode voice : candidateMap.values()) {
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
        candidate voices, pick the best matching voice_id. Be selective — only pick a voice if
        it genuinely fits the description. If none is a good match, return the word null.
        Respond with ONLY the voice_id string or the word null. No explanation.
        """;
    String user = "Required voice: " + voiceDescription + "\n\nCandidates:\n" + voiceList;

    String response = chatService.call(PROMPT_ENG_MODEL, system, user);
    if (response == null || response.isBlank() || "null".equalsIgnoreCase(response.strip())) {
      log.info("[ElevenLabs] Library match: Haiku found no suitable voice, will design a new one");
      return Optional.empty();
    }

    String matchedId = response.strip();
    Optional<JsonNode> matched = candidateMap.values().stream()
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
   * Parses the keyword queries returned by Haiku. Expects a JSON array of strings; falls back to
   * comma-splitting if JSON parsing fails.
   */
  private List<String> parseKeywordQueries(String response) {
    if (response == null || response.isBlank()) {
      return List.of();
    }
    try {
      JsonNode node = objectMapper.readTree(
          com.example.hypocaust.common.JsonUtils.extractJson(response));
      if (node.isArray()) {
        List<String> queries = new ArrayList<>();
        for (JsonNode item : node) {
          String q = item.asText().strip();
          if (!q.isBlank()) {
            queries.add(q);
          }
        }
        return queries;
      }
    } catch (Exception e) {
      log.debug("[ElevenLabs] Could not parse keyword JSON, falling back to comma-split: {}",
          e.getMessage());
    }
    // Fallback: comma-split
    return List.of(response.split(",")).stream()
        .map(String::strip)
        .filter(s -> !s.isBlank())
        .limit(3)
        .toList();
  }

  /**
   * Build TTS phases.
   *
   * <ul>
   *   <li>If {@code voice_id} is present: single-phase direct TTS.</li>
   *   <li>Otherwise: delegate voice resolution entirely to {@link #buildVoiceDesignPhases}, which
   *       handles library search and optional voice generation. A final TTS phase is appended that
   *       uses the {@code voiceId} from the first preview returned by voice design.</li>
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
