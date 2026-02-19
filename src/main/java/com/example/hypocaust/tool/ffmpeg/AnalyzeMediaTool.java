package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Analyzes a media file via the FFmpeg sidecar. Returns codec info, duration, bitrate, resolution,
 * loudness measurements (EBU R128), and other technical metadata. Use this before applying
 * corrections or to inspect unknown media.
 */
@RequiredArgsConstructor
@Slf4j
public class AnalyzeMediaTool {

  private final FfmpegClient ffmpegClient;

  @DiscoverableTool(
      name = "analyze_media",
      description = "Analyze a media file to get technical metadata: codec, duration, bitrate, "
          + "resolution, loudness (EBU R128), frame rate, sample rate, and channel layout. "
          + "Use this to inspect media before processing or to measure loudness levels.")
  public FfmpegResult analyze(
      @ToolParam(description = "URL of the media file to analyze") String mediaUrl
  ) {
    if (mediaUrl == null || mediaUrl.isBlank()) {
      return FfmpegResult.error("Media URL is required");
    }

    log.info("Analyzing media: {}", mediaUrl);

    try {
      var result = ffmpegClient.analyze(mediaUrl);
      return FfmpegResult.analysisSuccess(result, "Media analysis complete");
    } catch (Exception e) {
      log.error("Media analysis failed: {}", e.getMessage(), e);
      return FfmpegResult.error("Analysis failed: " + e.getMessage());
    }
  }
}
