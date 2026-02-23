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
    byte[] audio = restClient.post()
        .uri("/text-to-speech")
        .contentType(MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .body(input)
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
        .uri("/voice-design")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(input)
        .retrieve()
        .body(ObjectNode.class);
  }

  public JsonNode dubbing(JsonNode input) {
    return restClient.post()
        .uri("/dubbing")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(input)
        .retrieve()
        .body(ObjectNode.class);
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
