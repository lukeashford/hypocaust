package com.example.hypocaust.models.assembly;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.assemblyai.api-key")
@Slf4j
public class AssemblyAiClient {

  private static final String BASE_URL = "https://api.assemblyai.com/v2";

  private final ObjectMapper objectMapper;

  @Value("${app.assemblyai.api-key}")
  private String apiKey;

  private RestClient restClient;

  @PostConstruct
  void init() {
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Authorization", apiKey)
        .build();
  }

  public JsonNode transcribe(JsonNode input) {
    ObjectNode created = restClient.post()
        .uri("/transcript")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(input)
        .retrieve()
        .body(ObjectNode.class);
    if (created == null) {
      return objectMapper.createObjectNode();
    }
    String id = created.path("id").asText(null);
    if (id == null) {
      return created;
    }
    return pollTranscript(id);
  }

  public JsonNode transcribeWithIntelligence(JsonNode input) {
    // Reuse same endpoint; the caller provides intelligence flags in input
    return transcribe(input);
  }

  private ObjectNode pollTranscript(String id) {
    int attempts = 0;
    while (attempts++ < 120) { // up to 2 minutes
      ObjectNode tr = restClient.get()
          .uri("/transcript/{id}", id)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(ObjectNode.class);
      if (tr == null) {
        break;
      }
      String s = tr.path("status").asText("");
      if ("completed".equalsIgnoreCase(s) || "error".equalsIgnoreCase(s)) {
        return tr;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return tr;
      }
    }
    ObjectNode timeout = objectMapper.createObjectNode();
    timeout.put("id", id);
    timeout.put("status", "timeout");
    return timeout;
  }
}
