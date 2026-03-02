package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.IntentMapping;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.example.hypocaust.tool.AbstractArtifactTool;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Unified creative generation tool. The decomposer describes what it needs and this tool handles
 * everything internally: model selection via RAG, prompt engineering, title/description generation,
 * and provider API calling via the execution router.
 *
 * <p>On technical failure, automatically falls back to the next-best model from RAG results
 * before giving up.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class GenerateCreativeTool extends AbstractArtifactTool<GenerateCreativeResult> {

  private static final String LOG_PREFIX = "[CREATIVE]";
  private static final int MAX_MODEL_ATTEMPTS = 2;

  private final ModelEmbeddingRegistry modelRag;
  private final ExecutionRouter executionRouter;
  private final WordingService wordingService;
  private final ObjectMapper objectMapper;
  private final ArtifactResolver artifactResolver;

  private static final ThreadLocal<ModelSearchResult> currentModel = new ThreadLocal<>();
  private final ArtifactMapper artifactMapper;

  @DiscoverableTool(
      name = "generate_creative",
      description = "Generate or edit creative content (images, audio, video, text). "
          + "Describe what you need. If you have preferences (e.g., high quality, fast/cheap, "
          + "photorealistic, creative), include them in the task. To use existing artifacts "
          + "as inputs, refer to them using the @ prefix (e.g., 'Make @user_photo a cartoon').")
  public GenerateCreativeResult generate(
      @ToolParam(description = "What to generate or edit, in natural language") String task
  ) {
    log.info("{} request: {}", LOG_PREFIX, task);

    // Step 1: Find suitable models
    ModelRequirement req = wordingService.generateModelRequirement(task);
    var models = modelRag.search(req);
    if (models.isEmpty()) {
      return GenerateCreativeResult.error("No suitable model found for: " + req);
    }

    // Step 2: Try models in ranked order, fall back on failure
    var errors = new ArrayList<String>();
    var failedPlatforms = new java.util.LinkedHashSet<String>();
    for (var model : models.stream().limit(MAX_MODEL_ATTEMPTS).toList()) {
      try {
        currentModel.set(model);
        log.info("{} Trying {} (platform: {})", LOG_PREFIX, model.name(), model.platform());
        return orchestrate(task, model.outputs());
      } catch (Exception e) {
        log.warn("{} {} failed: {}", LOG_PREFIX, model.name(), e.getMessage());
        errors.add(model.name() + ": " + e.getMessage());
        failedPlatforms.add(model.platform());
      } finally {
        currentModel.remove();
      }
    }

    // Build an actionable error: tell the decomposer not to retry this capability
    String errorMsg =
        "All models failed. Providers attempted: " + String.join(", ", failedPlatforms)
            + ". Details: " + String.join("; ", errors) + ". "
            + "DO NOT retry generation with similar parameters — the underlying service appears unavailable.";
    return GenerateCreativeResult.error(errorMsg);
  }

  @Override
  protected List<Artifact> doExecute(String task, List<Artifact> gestating,
      List<IntentMapping> mappings) {
    ModelSearchResult model = currentModel.get();
    var executor = executionRouter.resolve(model.platform());

    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();

    // Delegate full pipeline to executor: plan → resolve artifact refs → execute → download/store

    var result = executor.run(gestating, task, model,
        input -> artifactResolver.resolve(input, availableArtifacts));

    // Validation: If the executor returns a list of artifacts that doesn't exactly match the count and kinds expected, throw an exception
    if (result.artifacts().size() != gestating.size()) {
      throw new IllegalStateException("Executor returned " + result.artifacts().size()
          + " artifacts, but expected " + gestating.size());
    }
    for (int i = 0; i < gestating.size(); i++) {
      if (result.artifacts().get(i).kind() != gestating.get(i).kind()) {
        throw new IllegalStateException(
            "Executor returned artifact of kind " + result.artifacts().get(i).kind()
                + " at index " + i + ", but expected " + gestating.get(i).kind());
      }
    }

    return result.artifacts().stream().map(finalized -> {
      // Merge generation_details into existing metadata (preserves executor-provided
      // fields like voiceId that were set via ExtractedOutput.metadata)
      var updated = finalized.withMetadata(
          mergeMetadata(finalized.metadata(), model, task, result.providerInput()));
      // Update the changelist with the finalized artifact
      TaskExecutionContextHolder.updateArtifact(updated);
      log.info("{} Complete: {} (status: {})", LOG_PREFIX, updated.name(),
          updated.status());
      return updated;
    }).toList();
  }

  @Override
  protected GenerateCreativeResult finalizeResult(List<Artifact> results,
      List<IntentMapping> mappings) {
    List<String> finalizedNames = results.stream().map(Artifact::name).toList();
    ModelSearchResult model = currentModel.get();
    String successSummary =
        "Generated artifacts: " + String.join(", ", finalizedNames) + " using "
            + model.name();
    return GenerateCreativeResult.success(finalizedNames, successSummary);
  }

  private ObjectNode buildMetadata(ModelSearchResult model, String task, JsonNode input) {
    ObjectNode metadata = objectMapper.createObjectNode();
    ObjectNode genDetails = metadata.putObject("generation_details");
    genDetails.put("provider", model.platform());
    genDetails.put("model_name", model.name());
    genDetails.put("owner", model.owner());
    genDetails.put("model_id", model.modelId());
    genDetails.put("prompt", task);
    if (input != null) {
      metadata.set("providerInput", input);
    }
    return metadata;
  }

  /**
   * Merge generation_details into existing artifact metadata without overwriting executor-provided
   * fields (e.g., voiceId set by ElevenLabs executor via ExtractedOutput).
   */
  private ObjectNode mergeMetadata(JsonNode existingMetadata, ModelSearchResult model, String task,
      JsonNode input) {
    ObjectNode genMeta = buildMetadata(model, task, input);
    if (existingMetadata != null && existingMetadata.isObject()) {
      // Start with existing metadata (preserves voiceId, etc.), then add generation_details
      ObjectNode merged = existingMetadata.deepCopy();
      merged.setAll(genMeta);
      return merged;
    }
    return genMeta;
  }
}
