package com.example.hypocaust.tool.creative;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.integration.ReplicateClient;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.rag.PlatformEmbeddingRegistry;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Unified creative generation tool. The decomposer describes what it needs and this tool handles
 * everything internally: model selection via RAG, prompt engineering, title/description generation,
 * and Replicate API calling.
 *
 * <p>Replaces all hardcoded creative operators (ImageGeneration, ImageEdit,
 * CreativeTextGeneration, ImagePromptEngineer).
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class GenerateCreativeTool {

  private static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final PlatformEmbeddingRegistry modelRag;
  private final ModelRegistry modelRegistry;
  private final ReplicateClient replicateClient;
  private final ObjectMapper objectMapper;

  @DiscoverableTool(
      name = "generate_creative",
      description = "Generate or edit creative content (images, audio, video, text). "
          + "Describe what you need. To use existing artifacts as inputs, refer to them "
          + "using the @ prefix (e.g., 'Make @user_photo a cartoon').")
  public GenerateCreativeResult generate(
      @ToolParam(description = "What to generate or edit, in natural language") String task,
      @ToolParam(description = "Kind of artifact") ArtifactKind artifactKind
  ) {
    log.info("Creative generation request: {} (kind: {})", task, artifactKind);

    // Step 1: Model selection via RAG
    var modelResults = modelRag.search(task + " " + artifactKind);
    if (modelResults.isEmpty()) {
      return GenerateCreativeResult.error("No suitable model found for: " + task);
    }

    var bestModel = modelResults.getFirst();
    log.info("Selected model: {}", bestModel.name());

    // Step 2: Parse model info and fetch schema from Replicate
    var modelInfo = parseModelInfo(bestModel.text());
    JsonNode modelVersion;
    try {
      modelVersion = replicateClient.getModelVersion(
          modelInfo.owner(), modelInfo.name(), modelInfo.version());
    } catch (Exception e) {
      log.error("Failed to fetch model version from Replicate: {}", e.getMessage());
      return GenerateCreativeResult.error("Failed to fetch model details: " + e.getMessage());
    }

    JsonNode schema = modelVersion.path("openapi_schema");
    List<Artifact> availableArtifacts = TaskExecutionContextHolder.getContext().getArtifacts()
        .getAllWithChanges();

    // Step 3: Unified plan generation (Title, Description, Replicate Input)
    var plan = generatePlan(task, artifactKind, bestModel.text(), schema, availableArtifacts);

    if (plan.errorMessage() != null) {
      log.warn("Creative generation plan failed: {}", plan.errorMessage());
      return GenerateCreativeResult.error(plan.errorMessage());
    }

    // Step 4: Substitute artifact placeholders
    var finalInput = substituteArtifacts(plan.replicateInput(), availableArtifacts);

    // Step 5: Schedule artifact
    var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(artifactKind)
        .title(plan.title())
        .description(plan.description())
        .prompt(task) // Store original task as prompt context
        .model(bestModel.name())
        .status(ArtifactStatus.GESTATING)
        .build());

    log.info("Scheduled artifact: {}", artifactName);

    try {
      // Step 6: Call Replicate
      var output = replicateClient.predict(
          modelInfo.owner(), modelInfo.name(), modelInfo.version(), finalInput);

      // Step 7: Extract result URL/content
      var resultUrl = extractOutputUrl(output);

      if (resultUrl == null || resultUrl.isBlank() || "null".equals(resultUrl)) {
        throw new IllegalStateException(
            "Model returned no usable output URL (got: " + resultUrl + ")");
      }

      // Step 8: Update artifact
      ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("originalTask", task);
      metadata.set("replicateInput", finalInput);
      metadata.put("model", bestModel.name());

      TaskExecutionContextHolder.getContext().getArtifacts()
          .updatePending(Artifact.builder()
              .name(artifactName)
              .kind(artifactKind)
              .title(plan.title())
              .description(plan.description())
              .prompt(task)
              .model(bestModel.name())
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

  record GenerateCreativePlan(
      String title,
      String description,
      JsonNode replicateInput,
      String errorMessage
  ) {

  }

  private GenerateCreativePlan generatePlan(
      String task,
      ArtifactKind kind,
      String modelDocs,
      JsonNode schema,
      List<Artifact> artifacts
  ) {
    try {
      var chatClient = ChatClient.builder(modelRegistry.get(PROMPT_ENG_MODEL)).build();

      var artifactNames = artifacts.stream().map(Artifact::name).toList();

      var response = chatClient.prompt()
          .system("""
              You are an expert creative director and prompt engineer. Your goal is to prepare a generation plan \
              for a Replicate model based on a user's task.
              
              INPUTS PROVIDED:
              1. User Task: The natural language description of what to generate/edit.
              2. Model Docs: Contextual information about the selected model.
              3. OpenAPI Schema: The EXACT input schema required by the Replicate API.
              4. Available Artifacts: Names of artifacts currently in the project.
              
              YOUR RESPONSIBILITIES:
              1. Title & Description: Create a catchy title (max 60 chars) and a 1-2 sentence description.
              2. Input Mapping: Construct the 'replicateInput' object matching the provided OpenAPI schema.
                 - Optimize prompts for the best artistic results.
                 - Map user requirements to specific schema fields.
                 - If a field requires a URL/image and the user refers to an artifact, use '@artifact_name' as a placeholder.
              3. Validation:
                 - Ensure all REQUIRED fields in the schema are present.
                 - If the user task is missing information that is MANDATORY for the model and cannot be reasonably defaulted, \
                   provide a concise but precise 'errorMessage' explaining what's missing (e.g., "This model requires a video length").
                 - If you provide an 'errorMessage', 'replicateInput' should be null.
              
              OUTPUT:
              Return ONLY valid JSON:
              {
                "title": "...",
                "description": "...",
                "replicateInput": { ... },
                "errorMessage": null or "..."
              }
              """)
          .user(String.format("""
              Task: %s
              Kind: %s
              Artifacts: %s
              Model Docs: %s
              Schema: %s
              """, task, kind, artifactNames, modelDocs, schema))
          .call()
          .content();

      var node = objectMapper.readTree(response);
      return new GenerateCreativePlan(
          node.path("title").asText("Untitled"),
          node.path("description").asText(""),
          node.path("replicateInput"),
          node.path("errorMessage").isTextual() ? node.path("errorMessage").asText() : null
      );
    } catch (Exception e) {
      log.error("Failed to generate creative plan", e);
      return new GenerateCreativePlan("Untitled", "", null,
          "Plan generation failed: " + e.getMessage());
    }
  }

  record ModelInfo(String owner, String name, String version) {

  }

  ModelInfo parseModelInfo(String modelDocs) {
    var owner = "stability-ai";
    var name = "sdxl";
    var version = "";

    for (var line : modelDocs.split("\n")) {
      var trimmed = line.trim();
      if (trimmed.startsWith("Replicate:")) {
        var parts = trimmed.substring("Replicate:".length()).trim().split("/");
        if (parts.length == 2) {
          owner = parts[0].trim();
          name = parts[1].trim();
        }
      } else if (trimmed.startsWith("Version:")) {
        version = trimmed.substring("Version:".length()).trim();
      }
    }

    return new ModelInfo(owner, name, version);
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

  String extractOutputUrl(JsonNode output) {
    if (output.isTextual()) {
      return output.asText();
    }
    if (output.isArray() && !output.isEmpty()) {
      return output.get(0).asText();
    }
    if (output.has("url")) {
      return output.get("url").asText();
    }
    return output.toString();
  }
}
