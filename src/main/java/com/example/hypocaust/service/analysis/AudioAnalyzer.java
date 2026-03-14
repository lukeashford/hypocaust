package com.example.hypocaust.service.analysis;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.TranscriptionService;
import com.example.hypocaust.service.staging.PendingUpload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_TRANSCRIPT_SAMPLE = 2000;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String TRANSCRIPT_ANALYSIS_PROMPT =
      "You are analyzing a transcript from a user-uploaded audio file. "
          + "Based on the transcript below, " + AnalysisPrompts.outputFormatFor("audio");

  private static final String NON_SPEECH_PROMPT = """
      You are analyzing a user-uploaded audio file that contains no detectable speech. \
      Based on the metadata below, classify it and %s

      Metadata:
      """.formatted(AnalysisPrompts.outputFormatFor("audio"));

  private final TranscriptionService transcriptionService;
  private final ChatService chatService;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    Duration duration = extractDuration(upload);

    try {
      TranscriptionService.TranscriptionResult probe = transcriptionService.transcribeSample(
          upload.storageKey(), duration, 0.25, Duration.ofSeconds(15));

      if (probe.highConfidence()) {
        return analyzeAsDialog(upload, probe.text(), duration);
      }

      return analyzeAsNonSpeech(upload, duration);
    } catch (Exception e) {
      log.debug("Audio analysis failed for upload {}: {}", upload.dataPackageId(), e.getMessage());
      return buildFallback(duration, null);
    }
  }

  private AnalysisResult analyzeAsDialog(PendingUpload upload, String probeText,
      Duration duration) {
    String fullTranscript;
    try {
      fullTranscript = transcriptionService.transcribeFull(upload.storageKey());
    } catch (Exception e) {
      log.debug("Full transcription failed, using probe text: {}", e.getMessage());
      fullTranscript = probeText;
    }

    String sample = fullTranscript.length() > MAX_TRANSCRIPT_SAMPLE
        ? fullTranscript.substring(0, MAX_TRANSCRIPT_SAMPLE) : fullTranscript;
    String response = chatService.call(MODEL, TRANSCRIPT_ANALYSIS_PROMPT, sample);

    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.put("audioType", "DIALOG");
    metadata.put("transcript", fullTranscript);
    if (duration != null) {
      metadata.put("durationSeconds", duration.toSeconds());
    }

    AnalysisResult parsed = AnalysisResponseParser.parse(response, true, fullTranscript, metadata);
    return parsed.isFallback() ? buildFallback(duration, metadata) : parsed;
  }

  private AnalysisResult analyzeAsNonSpeech(PendingUpload upload, Duration duration) {
    String durationStr = duration != null ? formatDuration(duration) : "unknown length";
    String metadataText = "Duration: " + durationStr + "\nMIME type: " + upload.mimeType();

    String response = chatService.call(MODEL, NON_SPEECH_PROMPT, metadataText);

    String audioType = (duration != null && duration.toSeconds() < 5) ? "SFX" : "MUSIC";
    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.put("audioType", audioType);
    if (duration != null) {
      metadata.put("durationSeconds", duration.toSeconds());
    }

    AnalysisResult parsed = AnalysisResponseParser.parse(response, false, null, metadata);
    return parsed.isFallback() ? buildFallback(duration, metadata) : parsed;
  }

  private AnalysisResult buildFallback(Duration duration, JsonNode metadata) {
    String durationStr = duration != null ? formatDuration(duration) : "unknown length";
    return new AnalysisResult("uploaded_audio", "Uploaded Audio",
        "User-uploaded audio file (" + durationStr + ")", false, null, metadata);
  }

  private Duration extractDuration(PendingUpload upload) {
    // Duration may come from client-provided metadata in the future.
    // For now, return null — the transcription service handles any length.
    return null;
  }

  private String formatDuration(Duration duration) {
    long minutes = duration.toMinutes();
    long seconds = duration.toSecondsPart();
    return minutes > 0 ? "%d:%02d".formatted(minutes, seconds) : "%ds".formatted(seconds);
  }
}
