package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.integration.ExecutionRouter;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.service.TaskComplexityService;
import com.example.hypocaust.service.WordingService;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

  private final ModelEmbeddingRegistry modelRag;
  private final ExecutionRouter executionRouter;
  private final TaskComplexityService complexityService;
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
    log.info("Creative generation request: {} (kind: {})", task, artifactKind);

    // Step 0: Complexity analysis
    String requiredTier = complexityService.evaluate(task, artifactKind);

    // Step 1: Model selection via RAG
    var modelResults = modelRag.search(task + " " + artifactKind + " tier: " + requiredTier);
    if (modelResults.isEmpty()) {
      return GenerateCreativeResult.error("No suitable model found for: " + task);
    }

    var bestModel = modelResults.getFirst();
    log.info("Selected model: {} (platform: {})", bestModel.name(), bestModel.platform());

    // Step 2: Resolve executor for this model's platform
    var executor = executionRouter.resolve(bestModel.platform());

    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();

    // Step 3: Generate provider input via executor
    var plan = executor.generatePlan(task, artifactKind, bestModel.name(),
        bestModel.owner(), bestModel.modelId(), bestModel.description(),
        bestModel.bestPractices(), availableArtifacts);

    if (plan.hasError()) {
      log.warn("Creative generation plan failed: {}", plan.errorMessage());
      return GenerateCreativeResult.error(plan.errorMessage());
    }

    // Step 3b: Generate title and description via wording service
    String title = wordingService.generateArtifactTitle(task);
    String description = wordingService.generateArtifactDescription(task);

    // Step 4: Substitute artifact placeholders
    var finalInput = substituteArtifacts(plan.providerInput(), availableArtifacts);

    // Step 5: Schedule artifact
    var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(artifactKind)
        .title(title)
        .description(description)
        .status(ArtifactStatus.GESTATING)
        .build());

    log.info("Scheduled artifact: {}", artifactName);

    try {
      // Step 6: Call provider
      var output = executor.execute(bestModel.owner(), bestModel.modelId(), finalInput);

      // Step 7: Extract result URL/content
      var resultUrl = executor.extractOutputUrl(output);

      if (resultUrl == null || resultUrl.isBlank() || "null".equals(resultUrl)) {
        throw new IllegalStateException(
            "Model returned no usable output URL (got: " + resultUrl + ")");
      }

      // Step 8: Update artifact
      ObjectNode metadata = objectMapper.createObjectNode();
      ObjectNode genDetails = metadata.putObject("generation_details");
      genDetails.put("provider", bestModel.platform());
      genDetails.put("model_name", bestModel.name());
      genDetails.put("owner", bestModel.owner());
      genDetails.put("model_id", bestModel.modelId());
      genDetails.put("prompt", task);
      metadata.set("providerInput", finalInput);

      TaskExecutionContextHolder.getContext().getArtifacts()
          .updatePending(Artifact.builder()
              .name(artifactName)
              .kind(artifactKind)
              .title(title)
              .description(description)
              .url(resultUrl)
              .metadata(metadata)
              .status(ArtifactStatus.CREATED)
              .build());

      log.info("Creative generation complete: {}", artifactName);
      return GenerateCreativeResult.success(
          artifactName, "Generated " + artifactKind + " using " + bestModel.name());

    } catch (Exception e) {
      log.error("Creative generation failed: {}", e.getMessage(), e);
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
        if (artifact.url() != null) {
          jsonString = jsonString.replace("@" + artifact.name(), artifact.url());
        }
      }
      return objectMapper.readTree(jsonString);
    } catch (Exception e) {
      log.error("Failed to substitute artifact placeholders", e);
      return node;
    }
  }
}
