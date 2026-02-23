package com.example.hypocaust.models.replicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Simple HTTP client for the Replicate API. Creates predictions, polls for completion, and returns
 * output URLs/content.
 */
@Component
@Slf4j
public class ReplicateClient {

  private static final String BASE_URL = "https://api.replicate.com/v1";
  private static final long POLL_INTERVAL_MS = 2000;
  private static final long MAX_WAIT_MS = 300_000; // 5 minutes

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public ReplicateClient(
      @Value("${app.replicate.api-token:}") String apiToken,
      ObjectMapper objectMapper
  ) {
    this.objectMapper = objectMapper;
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Authorization", "Bearer " + apiToken)
        .defaultHeader("Content-Type", "application/json")
        .defaultHeader("Prefer", "wait")
        .build();
  }

  /**
   * Create and await a prediction on a Replicate model.
   *
   * @param modelOwner the model owner (e.g., "stability-ai")
   * @param modelName the model name (e.g., "sdxl")
   * @param version the model version hash
   * @param input JSON object with model-specific input parameters
   * @return the prediction output (URLs, text, etc.)
   */
  public JsonNode predict(String modelOwner, String modelName, String version, JsonNode input) {
    log.info("Creating prediction for {}/{} (version: {})", modelOwner, modelName, version);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("version", version);
    body.set("input", input);

    // Try with "Prefer: wait" header first (sync mode, up to 60s)
    var response = restClient.post()
        .uri("/predictions")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new ReplicateException("No response from Replicate API");
    }

    var status = response.path("status").asText();

    // If the prediction completed synchronously
    if ("succeeded".equals(status)) {
      log.info("Prediction completed synchronously");
      return response.path("output");
    }

    if ("failed".equals(status)) {
      throw new ReplicateException(
          "Prediction failed: " + response.path("error").asText("unknown error"));
    }

    // Poll for completion
    var predictionUrl = response.path("urls").path("get").asText();
    if (predictionUrl.isBlank()) {
      predictionUrl = BASE_URL + "/predictions/" + response.path("id").asText();
    }

    return awaitPrediction(predictionUrl);
  }

  /**
   * Resolve the latest version hash for a model.
   */
  public String getLatestVersion(String owner, String name) {
    log.info("Resolving latest version for {}/{}", owner, name);
    var response = restClient.get()
        .uri("/models/{owner}/{name}", owner, name)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new ReplicateException("No response from Replicate API when fetching model details");
    }

    String latestVersion = response.path("latest_version").path("id").asText();
    if (latestVersion.isBlank()) {
      throw new ReplicateException("Could not find latest_version for model " + owner + "/" + name);
    }
    return latestVersion;
  }

  /**
   * Fetch the OpenAPI schema for a specific model version.
   */
  public JsonNode getSchema(String owner, String name, String version) {
    log.info("Fetching schema for {}/{} (version: {})", owner, name, version);
    var response = restClient.get()
        .uri("/models/{owner}/{name}/versions/{version}", owner, name, version)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new ReplicateException("No response from Replicate API when fetching model version");
    }
    return response.path("openapi_schema");
  }

  private JsonNode awaitPrediction(String predictionUrl) {
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
      var response = restClient.get()
          .uri(predictionUrl)
          .retrieve()
          .body(JsonNode.class);

      if (response != null) {
        var status = response.path("status").asText();

        switch (status) {
          case "succeeded" -> {
            log.info("Prediction completed after {}ms",
                System.currentTimeMillis() - startTime);
            return response.path("output");
          }
          case "failed", "canceled" -> throw new ReplicateException(
              "Prediction " + status + ": " + response.path("error").asText("unknown error"));
          default -> log.debug("Prediction status: {}", status);
        }
      }

      try {
        //noinspection BusyWait
        Thread.sleep(POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ReplicateException("Prediction polling interrupted");
      }
    }

    throw new ReplicateException("Prediction timed out after " + MAX_WAIT_MS + "ms");
  }

  public static class ReplicateException extends RuntimeException {

    public ReplicateException(String message) {
      super(message);
    }
  }
}
