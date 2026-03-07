package com.example.hypocaust.models.runway;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.runway.api-key")
@Slf4j
public class RunwayModelExecutor extends AbstractModelExecutor {

  private final RunwayClient runwayClient;

  public RunwayModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ArtifactResolver artifactResolver, RunwayClient runwayClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService,
        artifactResolver);
    this.runwayClient = runwayClient;
  }

  @Override
  public Platform platform() {
    return Platform.RUNWAY;
  }

  private static final String RUNWAY_SYSTEM_PROMPT = """
      You are planning for a Runway cinematic video generation model.

      API INPUT SPEC — use EXACTLY these field names:
      - "promptText"  (string, REQUIRED) — the cinematic description of the scene and motion
      - "ratio"       (string, REQUIRED) — one of: "1280:720" | "720:1280" | "1104:832" |
                      "960:960" | "832:1104" | "1584:672". Choose based on the desired aspect
                      ratio: use "1280:720" for landscape/widescreen, "720:1280" for portrait.
      - "promptImage" (string URL, optional) — source image URL for image-to-video mode;
                      use '@artifact_name' if an image artifact is available.
      - "duration"    (integer, optional) — clip length in seconds; 5 or 10.

      Do NOT invent field names such as "prompt", "text", "image", etc. Only the names above
      are accepted by the Runway API.

      PROMPT STYLE:
      - Write promptText as a cinematographer's shot note: subject + camera move + lens +
        lighting + mood (e.g. "slow push-in toward weathered cliff face, anamorphic lens,
        golden-hour side light, atmospheric haze, contemplative").

      VALIDATION:
      - Both promptText and ratio are mandatory. If you cannot determine a ratio, default to
        "1280:720". Set errorMessage if any other required info is missing.

      OUTPUT KEY CONVENTIONS for outputMapping:
      - Use "video" as the output key for video generation results.
      """;

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts) {
    return generatePlanWithLlm(task, model, artifacts, RUNWAY_SYSTEM_PROMPT, null);
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    return switch (modelId) {
      case "gen4.5" -> {
        // Route to image-to-video endpoint when an image is provided, text-to-video otherwise
        if (input.has("promptImage")) {
          yield runwayClient.generateVideoFromImage(modelId, input);
        } else {
          yield runwayClient.generateVideo(modelId, input);
        }
      }
      case "upscale-v1" -> runwayClient.upscale(input);
      default -> {
        log.warn("Unknown Runway model ID: {}, attempting generic video generation", modelId);
        yield runwayClient.generateVideo(modelId, input);
      }
    };
  }

  @Override
  protected Map<String, ExtractedOutput> extractOutputs(JsonNode output) {
    if (output.has("url")) {
      return Map.of("video", ExtractedOutput.ofContent(output.get("url").asText()));
    }
    if (output.has("artifacts") && output.get("artifacts").isArray()
        && !output.get("artifacts").isEmpty()) {
      JsonNode first = output.get("artifacts").get(0);
      if (first.has("url")) {
        return Map.of("video", ExtractedOutput.ofContent(first.get("url").asText()));
      }
    }
    if (output.has("id")) {
      return Map.of("video", ExtractedOutput.ofContent(output.get("id").asText()));
    }
    if (output.has("output") && output.get("output").isArray()
        && !output.get("output").isEmpty()) {
      return Map.of("video", ExtractedOutput.ofContent(output.get("output").get(0).asText()));
    }
    return Map.of("video", ExtractedOutput.ofContent(output.toString()));
  }
}
