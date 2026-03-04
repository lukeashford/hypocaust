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
      You are planning for a fal.ai model.

      INPUT MAPPING:
      - Construct the 'providerInput' object matching the fal.ai model's expected input format.
      - Optimize prompts for the best artistic results.
      - If a field requires a URL/image and the user refers to an artifact, use '@artifact_name'
        as a placeholder.

      VALIDATION:
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
