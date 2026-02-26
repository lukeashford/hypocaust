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

  public JsonNode textToSpeech(JsonNode input) {
    String voiceId = input.has("voice_id") ? input.get("voice_id").asText()
        : "pMs2u0D27UcSQMey5Go3"; // Default voice

    // Wrap voice settings if present
    ObjectNode body = input.deepCopy();
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
    body.remove("voice_id");

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
    return restClient.post()
        .uri("/voice-generation/generate-voice")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(input)
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
