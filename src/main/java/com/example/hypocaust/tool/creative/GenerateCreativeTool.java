package com.example.hypocaust.tool.creative;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactDraft;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.integration.ReplicateClient;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.rag.PlatformEmbeddingRegistry;
import com.example.hypocaust.tool.registry.DiscoverableTool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * Unified creative generation tool. The decomposer describes what it needs and this tool
 * handles everything internally: model selection via RAG, prompt engineering, and Replicate
 * API calling.
 *
 * <p>Replaces all hardcoded creative operators (ImageGeneration, ImageEdit,
 * CreativeTextGeneration, ImagePromptEngineer).
 */
@DiscoverableTool(
    name = "generate_creative",
    description = "Generate or edit creative content (images, audio, video, text). "
        + "Handles model selection, prompt optimization, and generation automatically.")
@RequiredArgsConstructor
@Slf4j
public class GenerateCreativeTool {

  private static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;
  private static final int MODEL_RAG_RESULTS = 3;

  private final PlatformEmbeddingRegistry modelRag;
  private final ModelRegistry modelRegistry;
  private final ReplicateClient replicateClient;
  private final ObjectMapper objectMapper;

  @Tool(name = "generate_creative",
      description = "Generate creative content. Describe what you need and the tool "
          + "handles model selection, prompt engineering, and generation.")
  public String generate(
      @ToolParam(description = "What to generate or edit, in natural language") String task,
      @ToolParam(description = "Kind of artifact: IMAGE, AUDIO, VIDEO, or STRUCTURED_JSON") String artifactKind,
      @ToolParam(description = "Human-readable description for the artifact") String description
  ) {
    log.info("Creative generation request: {} (kind: {})", task, artifactKind);

    var kind = ArtifactKind.valueOf(artifactKind.toUpperCase());

    // Step 1: Model selection via RAG
    var modelResults = modelRag.search(task + " " + artifactKind);
    if (modelResults.isEmpty()) {
      return "{\"error\": \"No suitable model found for: " + task + "\"}";
    }

    var bestModel = modelResults.getFirst();
    log.info("Selected model: {}", bestModel.name());

    // Step 2: Prompt engineering - use the model docs to craft an optimized prompt
    var optimizedPrompt = engineerPrompt(task, bestModel.text());

    // Step 3: Parse model info from RAG result to get Replicate coordinates
    var modelInfo = parseModelInfo(bestModel.text());

    // Step 4: Schedule artifact
    var artifactName = TaskExecutionContextHolder.addArtifact(ArtifactDraft.builder()
        .kind(kind)
        .title(description != null ? description : task)
        .description(description != null ? description : task)
        .prompt(optimizedPrompt)
        .model(bestModel.name())
        .status(ArtifactStatus.GESTATING)
        .build());

    log.info("Scheduled artifact: {}", artifactName);

    try {
      // Step 5: Call Replicate
      var input = buildReplicateInput(optimizedPrompt, kind, bestModel.text());
      var output = replicateClient.predict(
          modelInfo.owner(), modelInfo.name(), modelInfo.version(), input);

      // Step 6: Extract result URL/content
      var resultUrl = extractOutputUrl(output);

      // Step 7: Update artifact
      ObjectNode metadata = objectMapper.createObjectNode();
      metadata.put("originalTask", task);
      metadata.put("optimizedPrompt", optimizedPrompt);
      metadata.put("model", bestModel.name());

      TaskExecutionContextHolder.getContext().getArtifacts()
          .updatePending(Artifact.builder()
              .name(artifactName)
              .kind(kind)
              .title(description != null ? description : task)
              .description(description != null ? description : task)
              .prompt(optimizedPrompt)
              .model(bestModel.name())
              .url(resultUrl)
              .metadata(metadata)
              .status(ArtifactStatus.CREATED)
              .build());

      log.info("Creative generation complete: {}", artifactName);
      return "{\"artifactName\": \"" + artifactName + "\", \"summary\": \"Generated "
          + kind + " using " + bestModel.name() + "\"}";

    } catch (Exception e) {
      log.error("Creative generation failed: {}", e.getMessage(), e);
      try {
        TaskExecutionContextHolder.getContext().getArtifacts().rollbackPending(artifactName);
      } catch (Exception rollbackEx) {
        log.warn("Failed to rollback artifact: {}", rollbackEx.getMessage());
      }
      return "{\"error\": \"Generation failed: " + e.getMessage().replace("\"", "'") + "\"}";
    }
  }

  private String engineerPrompt(String task, String modelDocs) {
    try {
      var chatClient = ChatClient.builder(modelRegistry.get(PROMPT_ENG_MODEL)).build();

      return chatClient.prompt()
          .system("""
              You are a prompt engineer. Given a creative task and model documentation,
              craft an optimized prompt that will produce the best results with that specific model.
              Return ONLY the optimized prompt, nothing else.
              """)
          .user("Task: " + task + "\n\nModel documentation:\n" + modelDocs)
          .call()
          .content();
    } catch (Exception e) {
      log.warn("Prompt engineering failed, using original task as prompt: {}", e.getMessage());
      return task;
    }
  }

  private record ModelInfo(String owner, String name, String version) {

  }

  private ModelInfo parseModelInfo(String modelDocs) {
    // Extract owner/name/version from model documentation
    // The RAG docs should contain lines like:
    // Replicate: owner/name
    // Version: abc123...
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

  private JsonNode buildReplicateInput(String prompt, ArtifactKind kind, String modelDocs) {
    var input = objectMapper.createObjectNode();
    input.put("prompt", prompt);

    // Add kind-specific defaults
    if (kind == ArtifactKind.IMAGE) {
      input.put("width", 1024);
      input.put("height", 1024);
    }

    return input;
  }

  private String extractOutputUrl(JsonNode output) {
    // Replicate outputs vary by model - could be a string URL, array of URLs, or object
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
