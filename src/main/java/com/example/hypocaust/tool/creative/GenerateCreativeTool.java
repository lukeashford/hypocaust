package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
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
public class GenerateCreativeTool {

  private static final String LOG_PREFIX = "[CREATIVE]";
  private static final int MAX_MODEL_ATTEMPTS = 2;

  private final ModelEmbeddingRegistry modelRag;
  private final ExecutionRouter executionRouter;
  private final WordingService wordingService;
  private final ObjectMapper objectMapper;
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
        return executeWithModel(task, model);
      } catch (Exception e) {
        log.warn("{} {} failed: {}", LOG_PREFIX, model.name(), e.getMessage());
        errors.add(model.name() + ": " + e.getMessage());
        failedPlatforms.add(model.platform());
      }
    }

    // Build an actionable error: tell the decomposer not to retry this capability
    String errorMsg =
        "All models failed. Providers attempted: " + String.join(", ", failedPlatforms)
            + ". Details: " + String.join("; ", errors) + ". "
            + "DO NOT retry generation with similar parameters — the underlying service appears unavailable.";
    return GenerateCreativeResult.error(errorMsg);
  }

  private GenerateCreativeResult executeWithModel(String task, ModelSearchResult model) {

    log.info("{} Trying {} (platform: {})", LOG_PREFIX, model.name(), model.platform());
    var executor = executionRouter.resolve(model.platform());

    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();

    List<Artifact> gestatingArtifacts = new ArrayList<>();
    List<String> artifactNames = new ArrayList<>();

    for (var outputSpec : model.outputs()) {
      var metadata = buildMetadata(model, task, null);

      // Schedule GESTATING artifact
      Artifact gestating = TaskExecutionContextHolder.addArtifact(
          task, outputSpec.getDescription(), outputSpec.getKind(), metadata);

      artifactNames.add(gestating.name());
      gestatingArtifacts.add(gestating);
    }

    try {
      // Delegate full pipeline to executor: plan → substitute → execute → download/store
      UnaryOperator<JsonNode> substitutor = input -> substituteArtifacts(input, availableArtifacts);
      var result = executor.run(gestatingArtifacts, task, model, substitutor);

      // Validation: If the executor returns a list of artifacts that doesn't exactly match the count and kinds expected, throw an exception
      if (result.artifacts().size() != gestatingArtifacts.size()) {
        throw new IllegalStateException("Executor returned " + result.artifacts().size()
            + " artifacts, but expected " + gestatingArtifacts.size());
      }
      for (int i = 0; i < gestatingArtifacts.size(); i++) {
        if (result.artifacts().get(i).kind() != gestatingArtifacts.get(i).kind()) {
          throw new IllegalStateException(
              "Executor returned artifact of kind " + result.artifacts().get(i).kind()
                  + " at index " + i + ", but expected " + gestatingArtifacts.get(i).kind());
        }
      }

      List<String> finalizedNames = new ArrayList<>();
      for (var finalized : result.artifacts()) {
        // Update metadata with providerInput
        var updated = finalized.withMetadata(
            buildMetadata(model, task, result.providerInput()));
        // Update the changelist with the finalized artifact
        TaskExecutionContextHolder.updateArtifact(updated);
        finalizedNames.add(updated.name());
        log.info("{} Complete: {} (status: {})", LOG_PREFIX, updated.name(),
            updated.status());
      }

      String successSummary =
          "Generated artifacts: " + String.join(", ", finalizedNames) + " using "
              + model.name();
      return GenerateCreativeResult.success(finalizedNames, successSummary);

    } catch (Exception e) {
      for (String name : artifactNames) {
        rollbackArtifact(name);
      }
      throw e;
    }
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

  private void rollbackArtifact(String artifactName) {
    try {
      TaskExecutionContextHolder.rollbackArtifact(artifactName);
    } catch (Exception rollbackEx) {
      log.warn("Failed to rollback artifact: {}", rollbackEx.getMessage());
    }
  }

  JsonNode substituteArtifacts(JsonNode node, List<Artifact> artifacts) {
    try {
      String jsonString = node.toString();
      for (Artifact artifact : artifacts) {
        String placeholder = "@" + artifact.name();
        String content;
        if (artifact.kind() == ArtifactKind.TEXT) {
          content = artifact.description();
        } else if (artifact.storageKey() != null) {
          content = artifactMapper.toPresignedUrl(artifact.storageKey());
        } else {
          continue;
        }
        if (content != null) {
          jsonString = jsonString.replace(placeholder, content);
        }
      }
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      log.error("Failed to substitute artifact placeholders", e);
      return node;
    }
  }
}
