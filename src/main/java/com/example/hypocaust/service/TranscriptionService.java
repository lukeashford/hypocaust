package com.example.hypocaust.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TranscriptionService {

  private static final String WHISPER_ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";
  private static final String WHISPER_MODEL = "whisper-1";
  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(60);

  private final StorageService storageService;
  private final String apiKey;
  private final HttpClient httpClient;

  public TranscriptionService(StorageService storageService,
      @Value("${app.llm.open-ai.api-key}") String apiKey) {
    this.storageService = storageService;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
  }

  public record TranscriptionResult(String text, boolean highConfidence) {

  }

  public TranscriptionResult transcribeSample(String storageKey, Duration totalDuration,
      double startFraction, Duration sampleLength) {
    // For now, transcribe the full file — clipping requires FFmpeg.
    // The Whisper API handles the audio decoding internally.
    byte[] audioBytes = storageService.fetch(storageKey);
    String text = callWhisper(audioBytes);
    boolean highConfidence = text != null && !text.isBlank() && text.split("\\s+").length > 3;
    return new TranscriptionResult(text != null ? text : "", highConfidence);
  }

  public String transcribeFull(String storageKey) {
    byte[] audioBytes = storageService.fetch(storageKey);
    String text = callWhisper(audioBytes);
    return text != null ? text : "";
  }

  private String callWhisper(byte[] audioBytes) {
    try {
      String boundary = "----FormBoundary" + UUID.randomUUID().toString().replace("-", "");

      byte[] body = buildMultipartBody(boundary, audioBytes);

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(WHISPER_ENDPOINT))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "multipart/form-data; boundary=" + boundary)
          .timeout(HTTP_TIMEOUT)
          .POST(HttpRequest.BodyPublishers.ofByteArray(body))
          .build();

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        log.warn("Whisper API returned {}: {}", response.statusCode(), response.body());
        return null;
      }

      // Response is JSON: {"text": "..."}
      String responseBody = response.body();
      int textStart = responseBody.indexOf("\"text\"");
      if (textStart < 0) {
        return null;
      }
      int valueStart = responseBody.indexOf('"', textStart + 6) + 1;
      int valueEnd = responseBody.lastIndexOf('"');
      if (valueStart <= 0 || valueEnd <= valueStart) {
        return null;
      }
      return responseBody.substring(valueStart, valueEnd);

    } catch (Exception e) {
      log.warn("Whisper transcription failed: {}", e.getMessage());
      return null;
    }
  }

  private byte[] buildMultipartBody(String boundary, byte[] audioBytes) {
    String prefix = "--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"file\"; filename=\"audio.mp3\"\r\n"
        + "Content-Type: audio/mpeg\r\n\r\n";
    String modelPart = "\r\n--" + boundary + "\r\n"
        + "Content-Disposition: form-data; name=\"model\"\r\n\r\n"
        + WHISPER_MODEL + "\r\n";
    String suffix = "--" + boundary + "--\r\n";

    byte[] prefixBytes = prefix.getBytes(StandardCharsets.UTF_8);
    byte[] modelBytes = modelPart.getBytes(StandardCharsets.UTF_8);
    byte[] suffixBytes = suffix.getBytes(StandardCharsets.UTF_8);

    byte[] body = new byte[prefixBytes.length + audioBytes.length + modelBytes.length
        + suffixBytes.length];
    int pos = 0;
    System.arraycopy(prefixBytes, 0, body, pos, prefixBytes.length);
    pos += prefixBytes.length;
    System.arraycopy(audioBytes, 0, body, pos, audioBytes.length);
    pos += audioBytes.length;
    System.arraycopy(modelBytes, 0, body, pos, modelBytes.length);
    pos += modelBytes.length;
    System.arraycopy(suffixBytes, 0, body, pos, suffixBytes.length);
    return body;
  }
}
