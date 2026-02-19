package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Converts a media file between formats or applies basic transformations: transcoding, scaling,
 * cropping, re-encoding, or extracting audio from video.
 */
@RequiredArgsConstructor
@Slf4j
public class ConvertMediaTool {

  private final FfmpegClient ffmpegClient;

  @DiscoverableTool(
      name = "convert_media",
      description = "Convert a media file to a different format or apply basic transformations. "
          + "Supports transcoding (e.g. MOV to MP4), scaling/resizing, cropping, "
          + "audio extraction from video, and codec changes. Provide FFmpeg filter arguments "
          + "for the desired transformation.")
  public FfmpegResult convert(
      @ToolParam(description = "URL of the input media file") String mediaUrl,
      @ToolParam(description = "FFmpeg arguments as a list "
          + "(e.g. [\"-vf\", \"scale=1920:1080\"] for scaling, "
          + "[\"-vn\", \"-acodec\", \"libmp3lame\"] for audio extraction)")
      List<String> ffmpegArgs
  ) {
    if (mediaUrl == null || mediaUrl.isBlank()) {
      return FfmpegResult.error("Media URL is required");
    }
    if (ffmpegArgs == null || ffmpegArgs.isEmpty()) {
      return FfmpegResult.error("FFmpeg arguments are required");
    }

    log.info("Converting media {} with args: {}", mediaUrl, ffmpegArgs);

    try {
      var result = ffmpegClient.convert(mediaUrl, ffmpegArgs);
      var outputUrl = result.path("output_url").asText(result.path("output").asText(""));
      return FfmpegResult.success(outputUrl, result, "Media conversion complete");
    } catch (Exception e) {
      log.error("Media conversion failed: {}", e.getMessage(), e);
      return FfmpegResult.error("Conversion failed: " + e.getMessage());
    }
  }
}
