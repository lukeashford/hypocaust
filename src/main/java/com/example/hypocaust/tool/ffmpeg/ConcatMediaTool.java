package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Concatenates multiple media files in sequence using FFmpeg's concat filter. All inputs must have
 * compatible codecs and dimensions for seamless joining.
 */
@RequiredArgsConstructor
@Slf4j
public class ConcatMediaTool {

  private final FfmpegClient ffmpegClient;
  private final ObjectMapper objectMapper;

  @DiscoverableTool(
      name = "concat_media",
      description = "Concatenate multiple media files into a single output. "
          + "Joins video/audio segments end-to-end in the specified order. "
          + "All inputs should have compatible formats. Use this for timeline assembly, "
          + "joining clips, or sequencing audio tracks.")
  public FfmpegResult concat(
      @ToolParam(description = "Ordered list of media file URLs to concatenate") List<String> mediaUrls
  ) {
    if (mediaUrls == null || mediaUrls.size() < 2) {
      return FfmpegResult.error("At least 2 media URLs are required for concatenation");
    }

    log.info("Concatenating {} media files", mediaUrls.size());

    try {
      // Build batch request for concat via the ffmpeg-api
      ObjectNode body = objectMapper.createObjectNode();
      body.put("input_url", mediaUrls.getFirst());

      // Add additional inputs and build concat filter
      ArrayNode extraInputs = body.putArray("extra_inputs");
      for (int i = 1; i < mediaUrls.size(); i++) {
        extraInputs.add(mediaUrls.get(i));
      }

      int n = mediaUrls.size();
      StringBuilder filter = new StringBuilder();
      for (int i = 0; i < n; i++) {
        filter.append(String.format("[%d:v][%d:a]", i, i));
      }
      filter.append(String.format("concat=n=%d:v=1:a=1[outv][outa]", n));

      body.put("filter_complex", filter.toString());
      body.putArray("ffmpeg_args")
          .add("-map").add("[outv]")
          .add("-map").add("[outa]");

      var result = ffmpegClient.execute("/api/v1/convert", body);
      var outputUrl = result.path("output_url").asText(result.path("output").asText(""));
      return FfmpegResult.success(outputUrl, result,
          "Concatenated " + n + " files into single output");
    } catch (Exception e) {
      log.error("Concatenation failed: {}", e.getMessage(), e);
      return FfmpegResult.error("Concatenation failed: " + e.getMessage());
    }
  }
}
