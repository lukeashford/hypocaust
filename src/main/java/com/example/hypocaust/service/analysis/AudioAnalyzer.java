package com.example.hypocaust.service.analysis;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.TranscriptionService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final Duration SHORT_THRESHOLD = Duration.ofSeconds(5);
  private static final Duration LONG_THRESHOLD = Duration.ofMinutes(3);
  private static final int MAX_TRANSCRIPT_SAMPLE = 2000;

  private static final String TRANSCRIPT_ANALYSIS_PROMPT = """
      You are analyzing a transcript from a user-uploaded audio file. Based on the transcript \
      below, respond with exactly three lines:
      name: <snake_case_name>
      title: <Human Readable Title>
      description: <One sentence describing what this audio contains>

      Examples: name: narration_intro, title: Narration Intro, \
      description: A narrator introduces the main character and setting.""";

  private final TranscriptionService transcriptionService;
  private final ChatService chatService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    Duration duration = extractDuration(entity);

    if (duration != null && duration.compareTo(SHORT_THRESHOLD) < 0) {
      return sfxResult(duration);
    }
    if (duration != null && duration.compareTo(LONG_THRESHOLD) > 0) {
      return longAudioResult(duration);
    }

    return classifyByTranscription(entity, duration);
  }

  private AnalysisResult classifyByTranscription(ArtifactEntity entity, Duration duration) {
    try {
      TranscriptionService.TranscriptionResult probe = transcriptionService.transcribeSample(
          entity.getStorageKey(), duration, 0.25, Duration.ofSeconds(15));

      if (probe.highConfidence()) {
        String fullTranscript = transcriptionService.transcribeFull(entity.getStorageKey());
        String sample = fullTranscript.length() > MAX_TRANSCRIPT_SAMPLE
            ? fullTranscript.substring(0, MAX_TRANSCRIPT_SAMPLE) : fullTranscript;
        String response = chatService.call(MODEL, TRANSCRIPT_ANALYSIS_PROMPT, sample);
        return AnalysisResponseParser.parse(response, true);
      }

      return longAudioResult(duration);
    } catch (Exception e) {
      log.debug("Transcription probe failed for artifact {}: {}", entity.getId(), e.getMessage());
      return durationFallback(duration);
    }
  }

  private Duration extractDuration(ArtifactEntity entity) {
    if (entity.getMetadata() == null) {
      return null;
    }
    var durationNode = entity.getMetadata().path("duration_seconds");
    if (durationNode.isMissingNode() || durationNode.isNull()) {
      return null;
    }
    return Duration.ofSeconds(durationNode.asLong());
  }

  private AnalysisResult sfxResult(Duration duration) {
    String durationStr = formatDuration(duration);
    return new AnalysisResult("sound_effect", "Sound Effect",
        "Short audio clip (" + durationStr + ")", false);
  }

  private AnalysisResult longAudioResult(Duration duration) {
    String durationStr = duration != null ? formatDuration(duration) : "unknown length";
    return new AnalysisResult("audio_track", "Audio Track",
        "Audio file (" + durationStr + ")", false);
  }

  private AnalysisResult durationFallback(Duration duration) {
    if (duration != null) {
      return longAudioResult(duration);
    }
    return new AnalysisResult("uploaded_audio", "Uploaded Audio", "User-uploaded audio file",
        false);
  }

  private String formatDuration(Duration duration) {
    long minutes = duration.toMinutes();
    long seconds = duration.toSecondsPart();
    return minutes > 0 ? "%d:%02d".formatted(minutes, seconds) : "%ds".formatted(seconds);
  }
}
