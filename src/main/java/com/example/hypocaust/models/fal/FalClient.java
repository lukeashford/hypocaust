package com.example.hypocaust.models.fal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class FalClient {

  private static final String BASE_URL = "https://queue.fal.run";
  private static final long POLL_INTERVAL_MS = 2000;
  private static final long MAX_WAIT_MS = 1_800_000; // 30 minutes

  private final RestClient restClient;

  public FalClient(
      @Value("${app.fal.api-key:}") String apiKey
  ) {
    this.restClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Authorization", "Key " + apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  public JsonNode submit(String modelPath, JsonNode input) {
    log.info("Submitting fal.ai job for model: {}", modelPath);

    JsonNode response;
    try {
      response = restClient.post()
          .uri(uriBuilder -> uriBuilder.path("/" + modelPath).build())
          .contentType(MediaType.APPLICATION_JSON)
          .body(input)
          .retrieve()
          .body(JsonNode.class);
    } catch (RestClientResponseException e) {
      throw new FalException(
          e.getStatusCode().value() + " " + e.getStatusText()
              + ": " + e.getResponseBodyAsString());
    }

    if (response == null) {
      throw new FalException("No response from fal.ai API");
    }

    // Queued response — poll for completion
    if (response.has("request_id")) {
      var requestId = response.path("request_id").asText();
      var responseUrl = response.path("response_url").asText(null);
      return awaitResult(modelPath, requestId, responseUrl);
    }

    // Synchronous result
    return response;
  }

  private JsonNode awaitResult(String modelPath, String requestId, String responseUrl) {
    long startTime = System.currentTimeMillis();

    while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
      JsonNode response;
      try {
        response = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/" + modelPath + "/requests/" + requestId + "/status")
                .build())
            .retrieve()
            .body(JsonNode.class);
      } catch (RestClientResponseException e) {
        throw new FalException(
            "Status poll failed: " + e.getStatusCode().value() + " " + e.getStatusText()
                + ": " + e.getResponseBodyAsString());
      }

      if (response != null) {
        var status = response.path("status").asText();

        switch (status) {
          case "COMPLETED" -> {
            log.info("fal.ai job completed after {}ms",
                System.currentTimeMillis() - startTime);
            // Prefer the response_url provided by the queue if available
            var resultUri = (responseUrl != null && !responseUrl.isBlank())
                ? responseUrl
                : "/" + modelPath + "/requests/" + requestId;
            try {
              return restClient.get()
                  .uri(resultUri)
                  .retrieve()
                  .body(JsonNode.class);
            } catch (RestClientResponseException e) {
              throw new FalException(
                  "Result fetch failed: " + e.getStatusCode().value() + " " + e.getStatusText()
                      + ": " + e.getResponseBodyAsString());
            }
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
