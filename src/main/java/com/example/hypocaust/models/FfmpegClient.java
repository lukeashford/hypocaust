package com.example.hypocaust.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the Hypocaust FFmpeg API sidecar. Provides typed methods for common operations
 * (analyze, convert) and exposes the OpenAPI schema for the GeneralFfmpegTool fallback.
 *
 * @see com.example.hypocaust.tool.ffmpeg
 */
@Component
@Slf4j
public class FfmpegClient {

  private static final long POLL_INTERVAL_MS = 2000;
  private static final long MAX_WAIT_MS = 300_000; // 5 minutes

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  private volatile JsonNode cachedOpenApiSchema;

  public FfmpegClient(
      @Value("${app.ffmpeg.base-url:http://localhost:8100}") String baseUrl,
      @Value("${app.ffmpeg.api-key:}") String apiKey,
      ObjectMapper objectMapper
  ) {
    this.baseUrl = baseUrl;
    this.objectMapper = objectMapper;
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }

  /**
   * Analyze a media file: codec info, duration, bitrate, loudness, resolution, etc.
   *
   * @param mediaUrl URL of the media file to analyze
   * @return analysis result as JSON
   */
  public JsonNode analyze(String mediaUrl) {
    log.info("Analyzing media: {}", mediaUrl);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("input_url", mediaUrl);

    var response = restClient.post()
        .uri("/api/v1/analyze")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new FfmpegException("No response from FFmpeg API for analyze");
    }

    return handleAsyncResponse(response);
  }

  /**
   * Run an FFmpeg conversion/filter pipeline.
   *
   * @param inputUrl URL of the input media file
   * @param ffmpegArgs list of FFmpeg CLI arguments for the filter/conversion
   * @return conversion result as JSON (includes output URL)
   */
  public JsonNode convert(String inputUrl, List<String> ffmpegArgs) {
    log.info("Converting media: {} with args: {}", inputUrl, ffmpegArgs);

    ObjectNode body = objectMapper.createObjectNode();
    body.put("input_url", inputUrl);
    body.set("ffmpeg_args", objectMapper.valueToTree(ffmpegArgs));

    var response = restClient.post()
        .uri("/api/v1/convert")
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new FfmpegException("No response from FFmpeg API for convert");
    }

    return handleAsyncResponse(response);
  }

  /**
   * Submit an arbitrary request body to a given FFmpeg API endpoint. Used by the
   * GeneralFfmpegTool for operations that don't have a dedicated tool.
   *
   * @param endpoint the API path (e.g. "/api/v1/convert", "/api/v1/batch")
   * @param requestBody the full request body as JSON
   * @return the response as JSON
   */
  public JsonNode execute(String endpoint, JsonNode requestBody) {
    log.info("Executing ffmpeg operation at {}", endpoint);

    var response = restClient.post()
        .uri(endpoint)
        .contentType(MediaType.APPLICATION_JSON)
        .body(requestBody)
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new FfmpegException("No response from FFmpeg API at " + endpoint);
    }

    return handleAsyncResponse(response);
  }

  /**
   * Fetch the OpenAPI schema from the sidecar. Cached after first successful call.
   */
  public JsonNode getOpenApiSchema() {
    if (cachedOpenApiSchema != null) {
      return cachedOpenApiSchema;
    }

    log.info("Fetching OpenAPI schema from FFmpeg API at {}", baseUrl);

    var response = restClient.get()
        .uri("/openapi.json")
        .retrieve()
        .body(JsonNode.class);

    if (response == null) {
      throw new FfmpegException("No OpenAPI schema returned by FFmpeg API");
    }

    cachedOpenApiSchema = response;
    return response;
  }

  /**
   * Check if the FFmpeg API sidecar is reachable.
   */
  public boolean isHealthy() {
    try {
      var response = restClient.get()
          .uri("/api/v1/health")
          .retrieve()
          .body(JsonNode.class);
      return response != null;
    } catch (Exception e) {
      log.warn("FFmpeg API health check failed: {}", e.getMessage());
      return false;
    }
  }

  private JsonNode handleAsyncResponse(JsonNode response) {
    var status = response.path("status").asText("");

    // Completed synchronously
    if ("completed".equals(status) || "succeeded".equals(status)) {
      return response;
    }

    // If there's a job ID, poll for completion
    var jobId = response.path("job_id").asText(response.path("id").asText(""));
    if (!jobId.isEmpty()
        && ("processing".equals(status) || "queued".equals(status) || "pending".equals(status))) {
      return awaitJob(jobId);
    }

    // No async pattern detected — return as-is
    return response;
  }

  private JsonNode awaitJob(String jobId) {
    long startTime = System.currentTimeMillis();
    var jobUrl = "/api/v1/jobs/" + jobId;

    while (System.currentTimeMillis() - startTime < MAX_WAIT_MS) {
      var response = restClient.get()
          .uri(jobUrl)
          .retrieve()
          .body(JsonNode.class);

      if (response != null) {
        var status = response.path("status").asText();
        switch (status) {
          case "completed", "succeeded" -> {
            log.info("FFmpeg job {} completed after {}ms", jobId,
                System.currentTimeMillis() - startTime);
            return response;
          }
          case "failed", "error" -> throw new FfmpegException(
              "FFmpeg job " + status + ": " + response.path("error").asText("unknown error"));
          default -> log.debug("FFmpeg job {} status: {}", jobId, status);
        }
      }

      try {
        //noinspection BusyWait
        Thread.sleep(POLL_INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new FfmpegException("FFmpeg job polling interrupted");
      }
    }

    throw new FfmpegException("FFmpeg job " + jobId + " timed out after " + MAX_WAIT_MS + "ms");
  }

  public static class FfmpegException extends RuntimeException {

    public FfmpegException(String message) {
      super(message);
    }
  }
}
