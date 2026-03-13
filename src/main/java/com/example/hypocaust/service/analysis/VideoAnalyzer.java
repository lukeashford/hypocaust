package com.example.hypocaust.service.analysis;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  // Probe positions as fractions of total duration, in priority order.
  // 25% skips intros/logos. 50% is statistically most representative.
  // 75% catches back-loaded content. 10% is a last resort past any opening slate.
  private static final double[] PROBE_POSITIONS = {0.25, 0.50, 0.75, 0.10};

  private static final Set<String> INCONCLUSIVE_MARKERS = Set.of(
      "black", "blank", "dark screen", "no visible content", "text only",
      "loading", "title card", "slate", "logo");

  private static final String SYSTEM_PROMPT = """
      You are analyzing a single frame from a video. First, assess whether this frame shows \
      meaningful visual content (not a black screen, logo, slate, or loading screen).

      If the frame IS meaningful, respond with:
      conclusive: true
      name: <snake_case_name>
      title: <Human Readable Title>
      description: <One sentence describing what this video appears to show>

      If the frame is NOT meaningful (black, blank, logo, text-only), respond with:
      conclusive: false""";

  private final ChatService chatService;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    Duration duration = extractDuration(entity);

    for (double position : PROBE_POSITIONS) {
      try {
        byte[] frame = extractKeyframe(entity.getStorageKey(), duration, position);
        String response = chatService.callWithImage(MODEL, SYSTEM_PROMPT, frame, "image/jpeg");

        if (isConclusiveResponse(response)) {
          return AnalysisResponseParser.parse(response, false);
        }
      } catch (UnsupportedOperationException e) {
        // FFmpeg not yet integrated — fall through to fallback
        break;
      } catch (Exception e) {
        log.debug("Frame extraction failed at position {} for artifact {}: {}",
            position, entity.getId(), e.getMessage());
      }
    }

    return durationFallback(duration);
  }

  // TODO: Integrate with FFmpeg service when available.
  // Expected call: ffmpegService.extractFrame(storageKey, timestamp)
  // Returns JPEG bytes of a single frame at the given timestamp.
  // Command: ffmpeg -ss <timestamp> -i <input> -vframes 1 -f image2 pipe:1
  @SuppressWarnings("java:S112")
  private byte[] extractKeyframe(String storageKey, Duration duration, double position) {
    throw new UnsupportedOperationException(
        "Video keyframe extraction requires FFmpeg integration (not yet available)");
  }

  private boolean isConclusiveResponse(String response) {
    return response != null && response.contains("conclusive: true");
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

  private AnalysisResult durationFallback(Duration duration) {
    String durationStr = duration != null
        ? "%d:%02d".formatted(duration.toMinutes(), duration.toSecondsPart())
        : "unknown length";
    return new AnalysisResult("uploaded_video", "Uploaded Video",
        "User-uploaded video (" + durationStr + ")", false);
  }
}
