package com.example.hypocaust.integration.fal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class FalClient {

  private static final String BASE_URL = "https://queue.fal.run";
  private static final long POLL_INTERVAL_MS = 2000;
  private static final long MAX_WAIT_MS = 300_000; // 5 minutes

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final boolean enabled;

  public FalClient(
      @Value("${app.fal.api-key:}") String apiKey,
      ObjectMapper objectMapper
  ) {
    this.objectMapper = objectMapper;
    this.enabled = apiKey != null && !apiKey.isBlank();
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Authorization", "Key " + apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public JsonNode submit(String modelPath, JsonNode input) {
    log.info("Submitting fal.ai job for model: {}", modelPath);

    var response = restClient.post()
        .uri("/{modelPath}", modelPath)
        .contentType(MediaType.APPLICATION_JSON)
        .body(input)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new FalException("No response from fal.ai API");
    }

    // If status is present, it's a queued response — poll for completion
    if (response.has("request_id")) {
      var requestId = response.path("request_id").asText();
      return awaitResult(modelPath, requestId);
    }

    // Synchronous result
    return response;
  }

  private JsonNode awaitResult(String modelPath, String requestId) {
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
      var response = restClient.get()
          .uri("/{modelPath}/requests/{requestId}/status", modelPath, requestId)
          .retrieve()
          .body(JsonNode.class);

      if (response != null) {
        var status = response.path("status").asText();

        switch (status) {
          case "COMPLETED" -> {
            log.info("fal.ai job completed after {}ms",
                System.currentTimeMillis() - startTime);
            // Fetch the result
            return restClient.get()
                .uri("/{modelPath}/requests/{requestId}", modelPath, requestId)
                .retrieve()
                .body(JsonNode.class);
          }
          case "FAILED" -> throw new FalException(
              "fal.ai job failed: " + response.path("error").asText("unknown error"));
          default -> log.debug("fal.ai job status: {}", status);
        }
      }

      try {
        //noinspection BusyWait
        Thread.sleep(POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new FalException("fal.ai job polling interrupted");
      }
    }

    throw new FalException("fal.ai job timed out after " + MAX_WAIT_MS + "ms");
  }

  public static class FalException extends RuntimeException {

    public FalException(String message) {
      super(message);
    }
  }
}
