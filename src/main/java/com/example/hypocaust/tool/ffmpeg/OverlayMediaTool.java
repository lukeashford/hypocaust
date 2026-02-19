package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Composites one media layer on top of another using FFmpeg's overlay filter. Supports positioning,
 * alpha blending, and picture-in-picture arrangements.
 */
@RequiredArgsConstructor
@Slf4j
public class OverlayMediaTool {

  private final FfmpegClient ffmpegClient;

  @DiscoverableTool(
      name = "overlay_media",
      description = "Composite/overlay one video or image on top of another. "
          + "Supports positioning (x, y offsets), alpha blending, watermarks, "
          + "picture-in-picture, and layered compositing. "
          + "The overlay input is placed on top of the base input.")
  public FfmpegResult overlay(
      @ToolParam(description = "URL of the base/background media file") String baseUrl,
      @ToolParam(description = "URL of the overlay/foreground media file") String overlayUrl,
      @ToolParam(description = "X position of overlay (e.g. '10', '(W-w)/2' for centered, "
          + "'W-w-10' for right-aligned). Default: 0") String x,
      @ToolParam(description = "Y position of overlay (e.g. '10', '(H-h)/2' for centered, "
          + "'H-h-10' for bottom-aligned). Default: 0") String y
  ) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return FfmpegResult.error("Base media URL is required");
    }
    if (overlayUrl == null || overlayUrl.isBlank()) {
      return FfmpegResult.error("Overlay media URL is required");
    }

    String xPos = (x != null && !x.isBlank()) ? x : "0";
    String yPos = (y != null && !y.isBlank()) ? y : "0";

    log.info("Overlaying {} onto {} at ({}, {})", overlayUrl, baseUrl, xPos, yPos);

    try {
      var ffmpegArgs = List.of(
          "-i", overlayUrl,
          "-filter_complex", String.format("[0:v][1:v]overlay=%s:%s", xPos, yPos)
      );

      var result = ffmpegClient.convert(baseUrl, ffmpegArgs);
      var outputUrl = result.path("output_url").asText(result.path("output").asText(""));
      return FfmpegResult.success(outputUrl, result, "Overlay composite complete");
    } catch (Exception e) {
      log.error("Overlay failed: {}", e.getMessage(), e);
      return FfmpegResult.error("Overlay failed: " + e.getMessage());
    }
  }
}
