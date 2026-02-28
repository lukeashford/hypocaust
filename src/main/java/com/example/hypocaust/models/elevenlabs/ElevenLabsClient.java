package com.example.hypocaust.models.elevenlabs;

import com.example.hypocaust.service.ContentStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.elevenlabs.api-key")
@Slf4j
public class ElevenLabsClient {

  private static final String BASE_URL = "https://api.elevenlabs.io/v1";

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

  private static final String DEFAULT_VOICE_ID = "pMs2u0D27UcSQMey5Go3";
  private static final String DEFAULT_TTS_MODEL = "eleven_v3";

  public JsonNode textToSpeech(JsonNode input) {
    String voiceId = DEFAULT_VOICE_ID;
    if (input.has("voice_id")) {
      String candidate = input.get("voice_id").asText();
      // Guard against model IDs or descriptive strings leaking in as voice_id.
      // Real ElevenLabs voice IDs are 20-char alphanumeric strings.
      if (candidate.matches("^[A-Za-z0-9]{15,30}$")) {
        voiceId = candidate;
      } else {
        log.warn("Ignoring invalid voice_id '{}', using default", candidate);
      }
    }

    ObjectNode body = input.deepCopy();
    body.remove("voice_id");

    // Ensure a model_id is always present
    if (!body.has("model_id")) {
      body.put("model_id", DEFAULT_TTS_MODEL);
    }

    // Wrap voice settings if present
    if (body.has("stability") || body.has("similarity_boost") || body.has("style") || body.has(
        "use_speaker_boost")) {
      ObjectNode settings = objectMapper.createObjectNode();
      if (body.has("stability")) {
        settings.set("stability", body.remove("stability"));
      }
      if (body.has("similarity_boost")) {
        settings.set("similarity_boost", body.remove("similarity_boost"));
      }
      if (body.has("style")) {
        settings.set("style", body.remove("style"));
      }
      if (body.has("use_speaker_boost")) {
        settings.set("use_speaker_boost", body.remove("use_speaker_boost"));
      }
      body.set("voice_settings", settings);
    }

    byte[] audio = restClient.post()
        .uri("/text-to-speech/{voiceId}", voiceId)
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(body)
        .retrieve()
        .body(byte[].class);
    String url = contentStorage.put("tts-" + System.currentTimeMillis() + ".mp3", audio,
        MediaType.valueOf("audio/mpeg"));
    ObjectNode out = objectMapper.createObjectNode();
    out.put("url", url);
    return out;
  }

  public JsonNode voiceDesign(JsonNode input) {
    ObjectNode body = input.deepCopy();
    // Default to the multilingual TTV model if none specified
    if (!body.has("model_id")) {
      body.put("model_id", "eleven_multilingual_ttv_v2");
    }
    // Auto-generate preview text when no text is provided
    if (!body.has("text") || body.get("text").asText().isBlank()) {
      body.put("auto_generate_text", true);
    }

    return restClient.post()
        .uri("/text-to-voice/design")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
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
        .uri("/sound-generation")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(input)
        .retrieve()
        .body(byte[].class);
    String url = contentStorage.put("sound-" + System.currentTimeMillis() + ".mp3", audio,
        MediaType.valueOf("audio/mpeg"));
    ObjectNode out = objectMapper.createObjectNode();
    out.put("url", url);
    return out;
  }
}
