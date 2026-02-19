package com.example.hypocaust.tool.ffmpeg;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.integration.FfmpegClient;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Fallback FFmpeg tool that handles any FFmpeg operation not covered by a dedicated tool. Uses the
 * sidecar's OpenAPI schema to let an LLM construct the correct API request, eliminating hand-coded
 * parameter strings.
 *
 * <p>This tool should only be called when no specific FFmpeg tool (analyze_media, normalize_loudness,
 * overlay_media, concat_media, extract_thumbnail, convert_media) matches the desired operation.
 */
@RequiredArgsConstructor
@Slf4j
public class GeneralFfmpegTool {

  private static final AnthropicChatModelSpec PLANNER_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final FfmpegClient ffmpegClient;
  private final ModelRegistry modelRegistry;
  private final ObjectMapper objectMapper;

  @DiscoverableTool(
      name = "general_ffmpeg",
      description = "FALLBACK: Execute an arbitrary FFmpeg operation via the FFmpeg API sidecar. "
          + "Only use this tool if no specific tool exists for the desired operation "
          + "(check analyze_media, normalize_loudness, overlay_media, concat_media, "
          + "extract_thumbnail, convert_media first). "
          + "Describe what you need in plain language and this tool will construct the correct "
          + "API request using the sidecar's OpenAPI schema.")
  public FfmpegResult execute(
      @ToolParam(description = "Natural language description of the FFmpeg operation to perform, "
          + "e.g. 'Apply a 3D LUT color grade from this .cube file to the video' or "
          + "'Sidechain compress the music track using the voiceover as the key signal'")
      String task,
      @ToolParam(description = "URL of the primary input media file") String mediaUrl
  ) {
    if (task == null || task.isBlank()) {
      return FfmpegResult.error("Task description is required");
    }
    if (mediaUrl == null || mediaUrl.isBlank()) {
      return FfmpegResult.error("Media URL is required");
    }

    log.info("GeneralFfmpegTool invoked for task: {}", task);

    try {
      // 1. Fetch the sidecar's OpenAPI schema
      JsonNode openApiSchema;
      try {
        openApiSchema = ffmpegClient.getOpenApiSchema();
      } catch (Exception e) {
        log.warn("Could not fetch OpenAPI schema, falling back to basic convert: {}", e.getMessage());
        return FfmpegResult.error(
            "FFmpeg API is not reachable. Ensure the ffmpeg sidecar is running (./gradlew pods-create).");
      }

      // 2. Ask the LLM to construct the API request from the schema + task description
      var chatClient = ChatClient.builder(modelRegistry.get(PLANNER_MODEL)).build();

      var planResponse = chatClient.prompt()
          .system(buildSystemPrompt())
          .user(String.format("""
              Task: %s
              Input Media URL: %s

              OpenAPI Schema:
              %s
              """, task, mediaUrl, openApiSchema))
          .call()
          .content();

      var planJson = JsonUtils.extractJson(planResponse);
      var plan = objectMapper.readTree(planJson);

      // 3. Validate the plan
      if (plan.has("errorMessage") && !plan.path("errorMessage").isNull()) {
        return FfmpegResult.error(plan.path("errorMessage").asText());
      }

      var endpoint = plan.path("endpoint").asText("/api/v1/convert");
      var requestBody = plan.path("requestBody");

      if (requestBody == null || requestBody.isMissingNode() || requestBody.isNull()) {
        return FfmpegResult.error("LLM could not construct a valid request for: " + task);
      }

      // 4. Execute the request against the sidecar
      var result = ffmpegClient.execute(endpoint, requestBody);
      var outputUrl = result.path("output_url").asText(result.path("output").asText(""));

      if (outputUrl.isBlank()) {
        return FfmpegResult.analysisSuccess(result, "FFmpeg operation complete (no output file)");
      }

      return FfmpegResult.success(outputUrl, result, "FFmpeg operation complete: " + task);

    } catch (Exception e) {
      log.error("GeneralFfmpegTool failed: {}", e.getMessage(), e);
      return FfmpegResult.error("FFmpeg operation failed: " + e.getMessage());
    }
  }

  private String buildSystemPrompt() {
    return """
        You are an FFmpeg expert. Given a user's task description, an input media URL, and the \
        OpenAPI schema of an FFmpeg REST API, construct the correct API request.

        INPUTS PROVIDED:
        1. Task: Natural language description of the desired FFmpeg operation.
        2. Input Media URL: The URL of the primary input file.
        3. OpenAPI Schema: The full OpenAPI/Swagger schema of the FFmpeg API sidecar.

        YOUR RESPONSIBILITIES:
        1. Identify the correct API endpoint from the schema (usually /api/v1/convert).
        2. Construct a valid JSON request body matching the schema, including:
           - The input URL
           - The correct FFmpeg arguments/filters for the requested operation
        3. If the task is impossible or ambiguous, provide an errorMessage instead.

        OUTPUT:
        Return ONLY valid JSON with this structure:
        {
          "endpoint": "/api/v1/convert",
          "requestBody": { ... },
          "errorMessage": null
        }

        If the task cannot be fulfilled:
        {
          "endpoint": null,
          "requestBody": null,
          "errorMessage": "Explanation of why this cannot be done"
        }
        """;
  }
}
