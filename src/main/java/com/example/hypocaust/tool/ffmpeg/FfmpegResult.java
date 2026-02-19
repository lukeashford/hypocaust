package com.example.hypocaust.tool.ffmpeg;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Shared result record for all FFmpeg tools.
 *
 * @param outputUrl URL of the produced output file (null for analysis-only operations)
 * @param data structured data returned by the FFmpeg API (analysis results, job metadata, etc.)
 * @param summary human-readable summary of what was done
 * @param errorMessage error description if the operation failed
 */
public record FfmpegResult(String outputUrl, JsonNode data, String summary, String errorMessage) {

  public static FfmpegResult success(String outputUrl, JsonNode data, String summary) {
    return new FfmpegResult(outputUrl, data, summary, null);
  }

  public static FfmpegResult analysisSuccess(JsonNode data, String summary) {
    return new FfmpegResult(null, data, summary, null);
  }

  public static FfmpegResult error(String errorMessage) {
    return new FfmpegResult(null, null, null, errorMessage);
  }
}
