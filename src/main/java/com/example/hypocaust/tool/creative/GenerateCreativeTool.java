package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Unified creative generation tool. The decomposer describes what it needs and this tool handles
 * everything internally: model selection via RAG, prompt engineering, title/description generation,
 * and provider API calling via the execution router.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class GenerateCreativeTool {

  private static final String LOG_PREFIX = "[CREATIVE] ";

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

    // Step 1: Task-to-Requirement Rewriting
    ModelRequirement req = wordingService.generateModelRequirement(task, artifactKind);
    log.info("Model requirements: inputs={}, output={}, tier={}, search='{}'",
        req.inputs(), req.output(), req.tier(), req.searchString());

    // Step 2: Model selection via RAG
    var modelResults = modelRag.search(req);
    if (modelResults.isEmpty()) {
      return GenerateCreativeResult.error("No suitable model found matching requirements: " + req);
    }

    var bestModel = modelResults.getFirst();
    log.info("{} Selected model: {} (platform: {})", LOG_PREFIX, bestModel.name(),
        bestModel.platform());

    // Step 3: Resolve executor for this model's platform
    var executor = executionRouter.resolve(bestModel.platform());

    // Step 3: Generate provider input via executor (without artifacts)
    var plan = executor.generatePlan(task, artifactKind, bestModel.name(),
        bestModel.owner(), bestModel.modelId(), bestModel.description(),
        bestModel.bestPractices());

    if (plan.hasError()) {
      log.warn("{} Planning failed: {}", LOG_PREFIX, plan.errorMessage());
      return GenerateCreativeResult.error(plan.errorMessage());
    }

    // Step 3b: Generate title and description via wording service
    String title = wordingService.generateArtifactTitle(task);
    String description = wordingService.generateArtifactDescription(task);

    // Step 4: Substitute artifact placeholders
    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();
    var finalInput = substituteArtifacts(plan.providerInput(), availableArtifacts);

    // Step 5: Schedule artifact
    var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(artifactKind)
        .title(title)
        .description(description)
        .status(ArtifactStatus.GESTATING)
        .build());

    log.info("{} Scheduled artifact: {}", LOG_PREFIX, artifactName);

    try {
      // Step 6: Call provider
      var output = executor.execute(bestModel.owner(), bestModel.modelId(), finalInput);

      // Step 7: Prepare metadata
      ObjectNode metadata = objectMapper.createObjectNode();
      ObjectNode genDetails = metadata.putObject("generation_details");
      genDetails.put("provider", bestModel.platform());
      genDetails.put("model_name", bestModel.name());
      genDetails.put("owner", bestModel.owner());
      genDetails.put("model_id", bestModel.modelId());
      genDetails.put("prompt", task);
      metadata.set("providerInput", finalInput);

      var builder = Artifact.builder()
          .name(artifactName)
          .kind(artifactKind)
          .title(title)
          .description(description)
          .metadata(metadata);

      // Step 8: Extract result URL/content
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
      return GenerateCreativeResult.success(
          artifactName, "Generated " + artifactKind + " using " + bestModel.name());

    } catch (Exception e) {
      log.error("{} Failed: {}", LOG_PREFIX, e.getMessage(), e);
      try {
        TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
      } catch (Exception rollbackEx) {
        log.warn("Failed to rollback artifact: {}", rollbackEx.getMessage());
      }
      return GenerateCreativeResult.error("Generation failed: " + e.getMessage());
    }
  }

  JsonNode substituteArtifacts(JsonNode node, List<Artifact> artifacts) {
    try {
      String jsonString = node.toString();
      for (Artifact artifact : artifacts) {
        String placeholder = "@" + artifact.name();
        if (artifact.kind() == ArtifactKind.TEXT && artifact.inlineContent() != null) {
          // Resolve to inline content for text
          String content = artifact.inlineContent().isTextual()
              ? artifact.inlineContent().asText()
              : artifact.inlineContent().toString();
          jsonString = jsonString.replace(placeholder, content);
        } else if (artifact.url() != null) {
          // Resolve to URL for others
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
