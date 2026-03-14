package com.example.hypocaust.service.analysis;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.staging.PendingUpload;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoAnalyzer implements ArtifactContentAnalyzer {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private static final double[] PROBE_POSITIONS = {0.25, 0.50, 0.75, 0.10};

  private static final String SYSTEM_PROMPT = """
      You are analyzing a single frame from a video. First, assess whether this frame shows \
      meaningful visual content (not a black screen, logo, slate, or loading screen).

      If the frame IS meaningful, respond with:
      conclusive: true
      """ + AnalysisPrompts.outputFormatFor("video") + """

      If the frame is NOT meaningful (black, blank, logo, text-only), respond with:
      conclusive: false""";

  private final ChatService chatService;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    for (double position : PROBE_POSITIONS) {
      try {
        byte[] frame = extractKeyframe(upload.storageKey(), null, position);
        String response = chatService.callWithImage(MODEL, SYSTEM_PROMPT, frame, "image/jpeg");

        if (isConclusiveResponse(response)) {
          return AnalysisResponseParser.parse(response, false);
        }
      } catch (UnsupportedOperationException e) {
        break;
      } catch (Exception e) {
        log.debug("Frame extraction failed at position {} for upload {}: {}",
            position, upload.dataPackageId(), e.getMessage());
      }
    }

    return durationFallback(null);
  }

  // TODO: Integrate with FFmpeg service when available.
  @SuppressWarnings("java:S112")
  private byte[] extractKeyframe(String storageKey, Duration duration, double position) {
    throw new UnsupportedOperationException(
        "Video keyframe extraction requires FFmpeg integration (not yet available)");
  }

  private boolean isConclusiveResponse(String response) {
    return response != null && response.contains("conclusive: true");
  }

  private AnalysisResult durationFallback(Duration duration) {
    String durationStr = duration != null
        ? "%d:%02d".formatted(duration.toMinutes(), duration.toSecondsPart())
        : "unknown length";
    return new AnalysisResult("uploaded_video", "Uploaded Video",
        "User-uploaded video (" + durationStr + ")", false, null, null);
  }
}
