package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Normalizes audio loudness to a target level using the EBU R128 loudnorm filter. Defaults to
 * broadcast standard -14 LUFS (suitable for streaming platforms like Spotify/YouTube).
 */
@RequiredArgsConstructor
@Slf4j
public class NormalizeLoudnessTool {

  private static final String DEFAULT_TARGET_LUFS = "-14";
  private static final String DEFAULT_TRUE_PEAK = "-1";
  private static final String DEFAULT_LRA = "11";

  private final FfmpegClient ffmpegClient;

  @DiscoverableTool(
      name = "normalize_loudness",
      description = "Normalize audio loudness to a target LUFS level using EBU R128. "
          + "Defaults to -14 LUFS (streaming standard). Use this to ensure consistent "
          + "volume levels across audio/video files, or to meet platform loudness requirements.")
  public FfmpegResult normalize(
      @ToolParam(description = "URL of the audio or video file") String mediaUrl,
      @ToolParam(description = "Target integrated loudness in LUFS (default: -14)") String targetLufs
  ) {
    if (mediaUrl == null || mediaUrl.isBlank()) {
      return FfmpegResult.error("Media URL is required");
    }

    String lufs = (targetLufs != null && !targetLufs.isBlank()) ? targetLufs : DEFAULT_TARGET_LUFS;

    log.info("Normalizing loudness of {} to {} LUFS", mediaUrl, lufs);

    try {
      var ffmpegArgs = List.of(
          "-af", String.format("loudnorm=I=%s:TP=%s:LRA=%s", lufs, DEFAULT_TRUE_PEAK, DEFAULT_LRA)
      );

      var result = ffmpegClient.convert(mediaUrl, ffmpegArgs);
      var outputUrl = result.path("output_url").asText(result.path("output").asText(""));
      return FfmpegResult.success(outputUrl, result,
          "Loudness normalized to " + lufs + " LUFS");
    } catch (Exception e) {
      log.error("Loudness normalization failed: {}", e.getMessage(), e);
      return FfmpegResult.error("Loudness normalization failed: " + e.getMessage());
    }
  }
}
