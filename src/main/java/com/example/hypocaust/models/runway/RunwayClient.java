package com.example.hypocaust.models.runway;

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
@ConditionalOnProperty(name = "app.runway.api-key")
@Slf4j
public class RunwayClient {

  private static final String BASE_URL = "https://api.dev.runwayml.com/v1";

  private final ObjectMapper objectMapper;

  @Value("${app.runway.api-key}")
  private String apiKey;

  private RestClient restClient;

  @PostConstruct
  void init() {
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .defaultHeader("X-Runway-Version", "2024-11-06")
        .build();
  }

  public JsonNode generateVideo(String modelId, JsonNode input) {
    return submitAndPoll("/text_to_video", modelId, input);
  }

  public JsonNode generateVideoFromImage(String modelId, JsonNode input) {
    return submitAndPoll("/image_to_video", modelId, input);
  }

  public JsonNode upscale(JsonNode input) {
    return submitAndPoll("/tasks", "upscale-v1",
        input); // Upscale often still uses /tasks or similar
  }

  private JsonNode submitAndPoll(String endpoint, String model, JsonNode input) {
    ObjectNode payload = objectMapper.createObjectNode();
    if (input.isObject()) {
      payload.setAll((ObjectNode) input);
    }
    payload.put("model", model);

    ObjectNode created = restClient.post()
        .uri(endpoint)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(ObjectNode.class);

    if (created == null) {
      return payload; // fallback
    }

    String id = created.path("id").asText(null);
    if (id == null) {
      return created;
    }

    return pollTask(id);
  }

  private ObjectNode pollTask(String id) {
    int attempts = 0;
    while (attempts++ < 1800) { // ~30 minutes max
      ObjectNode status = restClient.get()
          .uri("/tasks/{id}", id)
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(ObjectNode.class);
      if (status == null) {
        break;
      }
      String s = status.path("status").asText("");
      if ("SUCCEEDED".equalsIgnoreCase(s) || "succeeded".equalsIgnoreCase(s)) {
        return status;
      }
      if ("FAILED".equalsIgnoreCase(s) || "ERROR".equalsIgnoreCase(s)) {
        return status;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return status;
      }
    }
    ObjectNode timeout = objectMapper.createObjectNode();
    timeout.put("id", id);
    timeout.put("status", "TIMEOUT");
    return timeout;
  }
}
