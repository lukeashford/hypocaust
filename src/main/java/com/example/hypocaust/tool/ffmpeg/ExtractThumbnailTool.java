package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Extracts a single frame from a video as an image. Useful for generating thumbnails, preview
 * images, or extracting specific frames for visual analysis.
 */
@RequiredArgsConstructor
@Slf4j
public class ExtractThumbnailTool {

  private final FfmpegClient ffmpegClient;

  @DiscoverableTool(
      name = "extract_thumbnail",
      description = "Extract a single frame from a video as an image file. "
          + "Specify a timestamp to pick the exact frame. Use this for generating thumbnails, "
          + "preview images, or grabbing a frame for visual inspection/analysis.")
  public FfmpegResult extract(
      @ToolParam(description = "URL of the video file") String videoUrl,
      @ToolParam(description = "Timestamp to extract (e.g. '00:00:05' or '5' for 5 seconds in). "
          + "Default: auto-select a representative frame") String timestamp
  ) {
    if (videoUrl == null || videoUrl.isBlank()) {
      return FfmpegResult.error("Video URL is required");
    }

    log.info("Extracting thumbnail from {} at {}", videoUrl, timestamp);

    try {
      List<String> ffmpegArgs;
      if (timestamp != null && !timestamp.isBlank()) {
        ffmpegArgs = List.of(
            "-ss", timestamp,
            "-frames:v", "1",
            "-f", "image2"
        );
      } else {
        // Use thumbnail filter to auto-select a representative frame
        ffmpegArgs = List.of(
            "-vf", "thumbnail",
            "-frames:v", "1",
            "-f", "image2"
        );
      }

      var result = ffmpegClient.convert(videoUrl, ffmpegArgs);
      var outputUrl = result.path("output_url").asText(result.path("output").asText(""));
      return FfmpegResult.success(outputUrl, result, "Thumbnail extracted");
    } catch (Exception e) {
      log.error("Thumbnail extraction failed: {}", e.getMessage(), e);
      return FfmpegResult.error("Thumbnail extraction failed: " + e.getMessage());
    }
  }
}
