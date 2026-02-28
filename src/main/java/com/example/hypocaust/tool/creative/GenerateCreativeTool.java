package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.SearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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

  @DiscoverableTool(
      name = "generate_creative",
      description = "Generate or edit creative content (images, audio, video, text). "
          + "Describe what you need. If you have preferences (e.g., high quality, fast/cheap, "
          + "photorealistic, creative), include them in the task. To use existing artifacts "
          + "as inputs, refer to them using the @ prefix (e.g., 'Make @user_photo a cartoon').")
  public GenerateCreativeResult generate(
      @ToolParam(description = "What to generate or edit, in natural language") String task,
      @ToolParam(description = "Kind of artifact") ArtifactKind artifactKind
  ) {
    log.info("{} request: {} (kind: {})", LOG_PREFIX, task, artifactKind);

    // Step 1: Find suitable models
    ModelRequirement req = wordingService.generateModelRequirement(task, artifactKind);
    var models = modelRag.search(req);
    if (models.isEmpty()) {
      return GenerateCreativeResult.error("No suitable model found for: " + req);
    }

    // Step 2: Try models in ranked order, fall back on failure
    var errors = new ArrayList<String>();
    var failedPlatforms = new java.util.LinkedHashSet<String>();
    for (var model : models.stream().limit(MAX_MODEL_ATTEMPTS).toList()) {
      try {
        return executeWithModel(task, artifactKind, model);
      } catch (Exception e) {
        log.warn("{} {} failed: {}", LOG_PREFIX, model.name(), e.getMessage());
        errors.add(model.name() + ": " + e.getMessage());
        failedPlatforms.add(model.platform());
      }
    }

    // Build an actionable error: tell the decomposer not to retry this capability
    var errorMsg = new StringBuilder("All models failed for " + artifactKind + " generation. ");
    errorMsg.append("Providers attempted: ").append(String.join(", ", failedPlatforms)).append(". ");
    errorMsg.append("Details: ").append(String.join("; ", errors)).append(". ");
    errorMsg.append("DO NOT retry ").append(artifactKind)
        .append(" generation with similar parameters — the underlying service appears unavailable.");
    return GenerateCreativeResult.error(errorMsg.toString());
  }

  private GenerateCreativeResult executeWithModel(
      String task, ArtifactKind artifactKind, SearchResult model) {

    log.info("{} Trying {} (platform: {})", LOG_PREFIX, model.name(), model.platform());
    var executor = executionRouter.resolve(model.platform());

    String title = wordingService.generateArtifactTitle(task);
    String description = wordingService.generateArtifactDescription(task);
    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();

    // Schedule artifact
    var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(artifactKind).title(title).description(description)
        .status(ArtifactStatus.GESTATING).build());

    try {
      // Delegate full pipeline to executor: plan → substitute → execute (with retry) → extract
      UnaryOperator<JsonNode> substitutor = input -> substituteArtifacts(input, availableArtifacts);
      var result = executor.run(task, artifactKind, model.name(),
          model.owner(), model.modelId(), model.description(), model.bestPractices(), substitutor);

      if (result.output() == null || result.output().isBlank() || "null".equals(result.output())) {
        throw new IllegalStateException("Model returned no usable output");
      }

      // Finalize artifact
      var builder = Artifact.builder()
          .name(artifactName).kind(artifactKind).title(title).description(description)
          .metadata(buildMetadata(model, task, result.providerInput()));

      if (artifactKind == ArtifactKind.TEXT) {
        builder.inlineContent(new TextNode(result.output())).status(ArtifactStatus.MANIFESTED);
      } else {
        builder.url(result.output()).status(ArtifactStatus.CREATED);
      }

      TaskExecutionContextHolder.getContext().getArtifacts().updatePending(builder.build());
      log.info("{} Complete: {}", LOG_PREFIX, artifactName);

      return GenerateCreativeResult.success(artifactName,
          "Generated " + artifactKind + " using " + model.name());

    } catch (Exception e) {
      rollbackArtifact(artifactName);
      throw e;
    }
  }

  private ObjectNode buildMetadata(SearchResult model, String task, JsonNode input) {
    ObjectNode metadata = objectMapper.createObjectNode();
    ObjectNode genDetails = metadata.putObject("generation_details");
    genDetails.put("provider", model.platform());
    genDetails.put("model_name", model.name());
    genDetails.put("owner", model.owner());
    genDetails.put("model_id", model.modelId());
    genDetails.put("prompt", task);
    metadata.set("providerInput", input);
    return metadata;
  }

  private void rollbackArtifact(String artifactName) {
    try {
      TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
    } catch (Exception rollbackEx) {
      log.warn("Failed to rollback artifact: {}", rollbackEx.getMessage());
    }
  }

  JsonNode substituteArtifacts(JsonNode node, List<Artifact> artifacts) {
    try {
      String jsonString = node.toString();
      for (Artifact artifact : artifacts) {
        String placeholder = "@" + artifact.name();
        String content = artifact.kind() == ArtifactKind.TEXT
            ? artifact.description()
            : artifact.url();
        jsonString = jsonString.replace(placeholder, content);
      }
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      log.error("Failed to substitute artifact placeholders", e);
      return node;
    }
  }
}
