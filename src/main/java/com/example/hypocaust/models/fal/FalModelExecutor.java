package com.example.hypocaust.models.fal;

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
@ConditionalOnProperty(name = "app.fal.api-key")
@Slf4j
public class FalModelExecutor extends AbstractModelExecutor {

  private final FalClient falClient;

  public FalModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ArtifactResolver artifactResolver, FalClient falClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService,
        artifactResolver);
    this.falClient = falClient;
  }

  @Override
  public Platform platform() {
    return Platform.FAL;
  }

  private static final String FAL_SYSTEM_PROMPT = """
      You are planning for a fal.ai model via the fal.ai Queue REST API.

      INPUT MAPPING:
      - Construct the 'providerInput' object matching the fal.ai model's expected input schema.
      - The object is sent as the raw JSON body — do NOT wrap it in an "input" key.
      - Optimize prompts for the best artistic results.

      FAL.AI STANDARD FIELD NAMES:
      - Text prompt: "prompt" (string, required for all generation models)
      - Source image: "image_url" (string URL — used by image-to-video and image-to-image models)
      - Source audio: "audio_url" (string URL — used by audio processing models)
      - Source video: "video_url" (string URL — used by video processing models)
      - Negative prompt: "negative_prompt" (string, optional)
      - Duration: "duration" (string, e.g. "5" or "10", for video models)
      - Aspect ratio: "aspect_ratio" (string, e.g. "16:9", for video models)
      - When an artifact reference resolves to a URL, place it in the appropriate field above.

      VALIDATION:
      - If a model requires an image input (image-to-video/image-to-image) but no image artifact
        is available, set 'errorMessage' explaining this.
      - If mandatory info is missing, provide an 'errorMessage'.

      OUTPUT KEY CONVENTIONS for outputMapping:
      - For image models: use "image" as the output key.
      - For video models: use "video" as the output key.
      - For audio models: use "audio" as the output key.
      - If unsure, use "output".
      """;

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts) {
    return generatePlanWithLlm(task, model, artifacts, FAL_SYSTEM_PROMPT, null);
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    var modelPath = owner + "/" + modelId;
    return falClient.submit(modelPath, input);
  }

  @Override
  protected Map<String, ExtractedOutput> extractOutputs(JsonNode output) {
    if (output.has("images") && output.get("images").isArray()
        && !output.get("images").isEmpty()) {
      return Map.of("image",
          ExtractedOutput.ofContent(output.get("images").get(0).path("url").asText()));
    }
    if (output.has("video") && output.get("video").has("url")) {
      return Map.of("video",
          ExtractedOutput.ofContent(output.get("video").path("url").asText()));
    }
    if (output.has("audio") && output.get("audio").has("url")) {
      return Map.of("audio",
          ExtractedOutput.ofContent(output.get("audio").path("url").asText()));
    }
    if (output.has("url")) {
      return Map.of("output", ExtractedOutput.ofContent(output.get("url").asText()));
    }
    return Map.of("output", ExtractedOutput.ofContent(output.toString()));
  }
}
