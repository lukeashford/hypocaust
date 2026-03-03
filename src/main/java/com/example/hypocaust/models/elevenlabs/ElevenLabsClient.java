package com.example.hypocaust.models.elevenlabs;

import com.example.hypocaust.service.ContentStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.elevenlabs.api-key")
@Slf4j
public class ElevenLabsClient {

  private static final String BASE_URL = "https://api.elevenlabs.io/v1";
  private static final String DEFAULT_TTS_MODEL = "eleven_v3";
  private static final String DEFAULT_TTV_MODEL = "eleven_ttv_v3";

  // Sensible defaults for voice settings
  private static final double DEFAULT_STABILITY = 0.5;
  private static final double DEFAULT_SIMILARITY_BOOST = 0.75;
  private static final double DEFAULT_STYLE = 0.0;
  private static final double DEFAULT_GUIDANCE_SCALE = 0.5;

  private final ObjectMapper objectMapper;
  private final ContentStorage contentStorage;

  @Value("${app.elevenlabs.api-key}")
  private String apiKey;

  private RestClient restClient;

  @PostConstruct
  void init() {
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("xi-api-key", apiKey)
        .build();
  }

  /**
   * Text-to-Speech. Requires voice_id in the input.
   * Returns {@code {"url": "..."}} with the stored audio URL.
   */
  public JsonNode textToSpeech(JsonNode input) {
    if (!input.has("voice_id") || input.get("voice_id").asText().isBlank()) {
      throw new IllegalArgumentException(
          "voice_id is required for Text-to-Speech. Use Voice Design (voice-design) "
              + "to generate a voice from a description, or provide a valid voice_id "
              + "from your ElevenLabs voice library.");
    }

    String candidate = input.get("voice_id").asText();
    if (!candidate.matches("^[A-Za-z0-9]{15,30}$")) {
      throw new IllegalArgumentException(
          "Invalid voice_id '" + candidate + "'. ElevenLabs voice IDs are 20-char "
              + "alphanumeric strings. Do not use descriptive text as a voice_id.");
    }
    String voiceId = candidate;

    ObjectNode body = input.deepCopy();
    body.remove("voice_id");
    // Remove fields that are not part of the TTS API
    body.remove("voice_description");

    if (!body.has("model_id")) {
      body.put("model_id", DEFAULT_TTS_MODEL);
    }

    // Build voice_settings with defaults for any unspecified values
    ObjectNode settings = objectMapper.createObjectNode();
    settings.put("stability",
        body.has("stability") ? body.remove("stability").doubleValue() : DEFAULT_STABILITY);
    settings.put("similarity_boost",
        body.has("similarity_boost") ? body.remove("similarity_boost").doubleValue()
            : DEFAULT_SIMILARITY_BOOST);
    settings.put("style",
        body.has("style") ? body.remove("style").doubleValue() : DEFAULT_STYLE);
    if (body.has("use_speaker_boost")) {
      settings.set("use_speaker_boost", body.remove("use_speaker_boost"));
    }
    body.set("voice_settings", settings);

    log.info("[ElevenLabs] TTS → POST /text-to-speech/{} | model={} | text_len={}",
        voiceId, body.path("model_id").asText(), body.path("text").asText().length());

    byte[] audio = restClient.post()
        .uri("/text-to-speech/{voiceId}", voiceId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(byte[].class);

    log.info("[ElevenLabs] TTS ← {} bytes received for voice_id={}", audio != null ? audio.length : 0, voiceId);
    return storeAudio("tts", audio);
  }

  /**
   * Voice Design. Returns all previews:
   * {@code {"previews": [{"url": "...", "generated_voice_id": "..."}, ...]}}
   */
  public JsonNode voiceDesign(JsonNode input) {
    ObjectNode body = input.deepCopy();
    // Remove fields that are not part of the voice design API
    body.remove("voice_id");

    // Always enforce the correct TTV model — the plan may have injected the TTS model_id
    body.put("model_id", DEFAULT_TTV_MODEL);
    // guidance_scale: controls how closely the AI follows the description (API field name)
    if (!body.has("guidance_scale")) {
      body.put("guidance_scale", DEFAULT_GUIDANCE_SCALE);
    }
    // Do NOT add 'quality' — only supported for eleven_multilingual_ttv_v2, not eleven_ttv_v3
    // Auto-generate preview text when no text is provided
    if (!body.has("text") || body.get("text").asText().isBlank()) {
      body.put("auto_generate_text", true);
    }

    log.info("[ElevenLabs] Voice Design → POST /text-to-voice/design | model={} | desc_len={} | text='{}'",
        body.path("model_id").asText(),
        body.path("voice_description").asText().length(),
        body.has("auto_generate_text") ? "(auto)" : body.path("text").asText());

    JsonNode response = restClient.post()
        .uri("/text-to-voice/design")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);

    log.debug("[ElevenLabs] Voice Design ← raw response keys: {}",
        response != null ? response.fieldNames() : "null");

    // API returns {"previews": [{"generated_voice_id": "...", "audio_base_64": "base64..."}]}
    // Store all previews' audio so results include playable URLs
    ObjectNode result = objectMapper.createObjectNode();
    ArrayNode previewsOut = result.putArray("previews");

    JsonNode previews = response != null ? response.path("previews") : null;
    if (previews != null && previews.isArray()) {
      log.info("[ElevenLabs] Voice Design ← {} preview(s) returned", previews.size());
      for (int i = 0; i < previews.size(); i++) {
        JsonNode preview = previews.get(i);
        ObjectNode entry = previewsOut.addObject();

        String genVoiceId = preview.path("generated_voice_id").asText();
        log.info("[ElevenLabs] Voice Design preview[{}] generated_voice_id='{}'", i, genVoiceId);
        if (!genVoiceId.isBlank()) {
          entry.put("generated_voice_id", genVoiceId);
        }
        // audio field is named audio_base_64 in the API response
        if (preview.has("audio_base_64")) {
          byte[] audioBytes = Base64.getDecoder().decode(preview.get("audio_base_64").asText());
          String url = contentStorage.put(
              "voice-preview-" + System.currentTimeMillis() + "-" + i + ".mp3",
              audioBytes, MediaType.valueOf("audio/mpeg"));
          entry.put("url", url);
          log.info("[ElevenLabs] Voice Design preview[{}] audio stored → {}", i, url);
        } else {
          log.warn("[ElevenLabs] Voice Design preview[{}] has no audio_base_64 field; available: {}", i, preview.fieldNames());
        }
      }
    } else {
      log.warn("[ElevenLabs] Voice Design ← no 'previews' array in response; response={}", response);
    }

    return result;
  }

  /**
   * Save a designed voice permanently.
   * Returns {@code {"voice_id": "permanent_id"}}.
   */
  public JsonNode saveVoice(String generatedVoiceId, String voiceName, String voiceDescription) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("voice_name", voiceName);
    body.put("voice_description", voiceDescription);
    body.put("generated_voice_id", generatedVoiceId);

    log.info("[ElevenLabs] Save Voice → POST /text-to-voice | generated_voice_id='{}' name='{}'",
        generatedVoiceId, voiceName);

    // Endpoint is POST /v1/text-to-voice (not /text-to-voice/create)
    JsonNode response = restClient.post()
        .uri("/text-to-voice")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);

    String permanentId = response != null ? response.path("voice_id").asText() : "null";
    log.info("[ElevenLabs] Save Voice ← generated_voice_id='{}' → permanent voice_id='{}'",
        generatedVoiceId, permanentId);

    return response;
  }

  /**
   * Search the user's voice library by keyword.
   * Returns the voices array from the API response.
   */
  public JsonNode searchVoices(String query) {
    JsonNode response = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/voices")
            .queryParam("search", query)
            .build())
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(JsonNode.class);

    return response != null ? response.path("voices") : objectMapper.createArrayNode();
  }

  public JsonNode dubbing(JsonNode input) {
    ObjectNode created = restClient.post()
        .uri("/dubbing")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(input)
        .retrieve()
        .body(ObjectNode.class);

    if (created == null || !created.has("dubbing_id")) {
      return created;
    }

    String dubbingId = created.get("dubbing_id").asText();
    return pollDubbing(dubbingId);
  }

  private JsonNode pollDubbing(String dubbingId) {
    long startTime = System.currentTimeMillis();
    long maxWait = 600_000; // 10 minutes for dubbing

    while (System.currentTimeMillis() - startTime < maxWait) {
      JsonNode status = restClient.get()
          .uri("/dubbing/{dubbingId}", dubbingId)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(JsonNode.class);

      if (status != null) {
        String s = status.path("status").asText();
        if ("finished".equalsIgnoreCase(s)) {
          return status;
        }
        if ("failed".equalsIgnoreCase(s)) {
          return status;
        }
      }

      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return status;
      }
    }
    ObjectNode timeout = objectMapper.createObjectNode();
    timeout.put("dubbing_id", dubbingId);
    timeout.put("status", "timeout");
    return timeout;
  }

  public JsonNode soundGeneration(JsonNode input) {
    byte[] audio = restClient.post()
        .uri("/text-to-sound-effects")
        .contentType(MediaType.APPLICATION_JSON)
        .body(input)
        .retrieve()
        .body(byte[].class);
    return storeAudio("sound", audio);
  }

  private ObjectNode storeAudio(String prefix, byte[] audio) {
    if (audio == null || audio.length == 0) {
      throw new IllegalStateException(
          "ElevenLabs returned empty audio. The API call succeeded but produced no audio data.");
    }
    String url = contentStorage.put(
        prefix + "-" + System.currentTimeMillis() + ".mp3",
        audio, MediaType.valueOf("audio/mpeg"));
    ObjectNode out = objectMapper.createObjectNode();
    out.put("url", url);
    return out;
  }
}
