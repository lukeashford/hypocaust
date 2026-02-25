package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.ExecutionAttempt;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.models.ModelExecutor;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.SearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Unified creative generation tool. The decomposer describes what it needs and this tool handles
 * everything internally: model selection via RAG, prompt engineering, title/description generation,
 * and provider API calling via the execution router.
 *
 * <p>On technical failure (provider error, network issue), automatically falls back to the
 * next-best model from RAG results before giving up. All attempts are recorded in the
 * {@link ExecutionReport} so the decomposer can make informed self-healing decisions.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class GenerateCreativeTool {

  private static final String LOG_PREFIX = "[CREATIVE] ";
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
    var reportBuilder = ExecutionReport.builder();

    // Step 1: Task-to-Requirement Rewriting
    ModelRequirement req = wordingService.generateModelRequirement(task, artifactKind);
    log.info("Model requirements: inputs={}, output={}, tier={}, search='{}'",
        req.inputs(), req.output(), req.tier(), req.searchString());

    // Step 2: Model selection via RAG (returns ranked list)
    var modelResults = modelRag.search(req);
    if (modelResults.isEmpty()) {
      String error = "No suitable model found matching requirements: " + req;
      reportBuilder.addAttempt(Map.of("step", "model_selection", "status", "failed",
          "error", error));
      return GenerateCreativeResult.error(error,
          reportBuilder.success(false).summary(error).build());
    }

    // Step 3: Try models in ranked order (fallback on technical failure)
    int modelsToTry = Math.min(MAX_MODEL_ATTEMPTS, modelResults.size());

    for (int modelIndex = 0; modelIndex < modelsToTry; modelIndex++) {
      var model = modelResults.get(modelIndex);
      log.info("{} Trying model {}/{}: {} (platform: {})", LOG_PREFIX,
          modelIndex + 1, modelsToTry, model.name(), model.platform());

      var result = tryModel(task, artifactKind, model, reportBuilder);
      if (result != null) {
        return result;
      }
      // If we get here, tryModel returned null meaning a technical failure occurred —
      // the attempt was already recorded, continue to next model
    }

    // All models exhausted
    String error = "All " + modelsToTry + " candidate models failed";
    return GenerateCreativeResult.error(error,
        reportBuilder.success(false).summary(error).build());
  }

  /**
   * Attempts generation with a single model. Returns the result on success or a non-recoverable
   * planning error. Returns null if the failure is technical and another model should be tried.
   */
  private GenerateCreativeResult tryModel(
      String task, ArtifactKind artifactKind, SearchResult model,
      ExecutionReport.Builder reportBuilder) {

    ModelExecutor executor;
    try {
      executor = executionRouter.resolve(model.platform());
    } catch (IllegalArgumentException e) {
      reportBuilder.addAttempt(Map.of(
          "step", "resolve_executor", "model", model.name(),
          "platform", model.platform(), "status", "failed",
          "error", e.getMessage()));
      return null; // try next model
    }

    // Generate plan
    var plan = executor.generatePlan(task, artifactKind, model.name(),
        model.owner(), model.modelId(), model.description(),
        model.bestPractices());

    if (plan.hasError()) {
      log.warn("{} Planning failed for {}: {}", LOG_PREFIX, model.name(), plan.errorMessage());
      reportBuilder.addAttempt(Map.of(
          "step", "plan", "model", model.name(),
          "platform", model.platform(), "status", "failed",
          "error", plan.errorMessage()));
      // Planning failures are model-specific — try the next model
      return null;
    }

    // Generate title and description
    String title = wordingService.generateArtifactTitle(task);
    String description = wordingService.generateArtifactDescription(task);

    // Substitute artifact placeholders
    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();
    var finalInput = substituteArtifacts(plan.providerInput(), availableArtifacts);

    // Schedule artifact
    var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(artifactKind)
        .title(title)
        .description(description)
        .status(ArtifactStatus.GESTATING)
        .build());

    log.info("{} Scheduled artifact: {}", LOG_PREFIX, artifactName);

    // Execute with retry (handles transient errors internally)
    ExecutionAttempt attempt = executor.executeWithRetry(
        model.owner(), model.modelId(), finalInput);

    // Record all execution attempts
    for (var attemptMeta : attempt.attempts()) {
      var enriched = new LinkedHashMap<>(attemptMeta);
      enriched.put("step", "execute");
      enriched.put("modelName", model.name());
      reportBuilder.addAttempt(enriched);
    }

    if (!attempt.succeeded()) {
      log.warn("{} Execution failed for model {}: {}", LOG_PREFIX, model.name(),
          attempt.lastError());
      // Rollback the artifact and let the caller try the next model
      rollbackArtifact(artifactName);
      return null;
    }

    // Extract output and finalize
    try {
      var output = attempt.output();

      // Prepare metadata
      ObjectNode metadata = objectMapper.createObjectNode();
      ObjectNode genDetails = metadata.putObject("generation_details");
      genDetails.put("provider", model.platform());
      genDetails.put("model_name", model.name());
      genDetails.put("owner", model.owner());
      genDetails.put("model_id", model.modelId());
      genDetails.put("prompt", task);
      metadata.set("providerInput", finalInput);

      var builder = Artifact.builder()
          .name(artifactName)
          .kind(artifactKind)
          .title(title)
          .description(description)
          .metadata(metadata);

      var result = executor.extractOutput(output);

      if (result == null || result.isBlank() || "null".equals(result)) {
        throw new IllegalStateException(
            "Model returned no usable output (got: " + result + ")");
      }
      if (artifactKind == ArtifactKind.TEXT) {
        builder.inlineContent(new TextNode(result))
            .status(ArtifactStatus.MANIFESTED);
      } else {
        builder.url(result)
            .status(ArtifactStatus.CREATED);
      }

      TaskExecutionContextHolder.getContext().getArtifacts()
          .updatePending(builder.build());

      log.info("{} Complete: {}", LOG_PREFIX, artifactName);

      String summary = "Generated " + artifactKind + " using " + model.name();
      reportBuilder.success(true).summary(summary).addArtifactName(artifactName);
      return GenerateCreativeResult.success(artifactName, summary, reportBuilder.build());

    } catch (Exception e) {
      log.error("{} Post-execution failed: {}", LOG_PREFIX, e.getMessage(), e);
      reportBuilder.addAttempt(Map.of(
          "step", "extract_output", "model", model.name(),
          "status", "failed", "error", e.getMessage()));
      rollbackArtifact(artifactName);
      return null;
    }
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
        if (artifact.kind() == ArtifactKind.TEXT && artifact.inlineContent() != null) {
          String content = artifact.inlineContent().isTextual()
              ? artifact.inlineContent().asText()
              : artifact.inlineContent().toString();
          jsonString = jsonString.replace(placeholder, content);
        } else if (artifact.url() != null) {
          jsonString = jsonString.replace(placeholder, artifact.url());
        }
      }
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      log.error("Failed to substitute artifact placeholders", e);
      return node;
    }
  }
}
