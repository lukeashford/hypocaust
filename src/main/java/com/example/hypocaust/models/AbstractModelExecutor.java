package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;

@Slf4j
public abstract class AbstractModelExecutor implements ModelExecutor {

  protected static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  protected final ModelRegistry modelRegistry;
  protected final ObjectMapper objectMapper;
  protected final ChatService chatService;
  protected final RetryTemplate retryTemplate;
  protected final StorageService storageService;

  protected AbstractModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService) {
    this.modelRegistry = modelRegistry;
    this.objectMapper = objectMapper;
    this.chatService = chatService;
    this.retryTemplate = retryTemplate;
    this.storageService = storageService;
  }

  /**
   * Subclasses implement this to prepare provider-specific input from the user task.
   */
  protected abstract ExecutionPlan generatePlan(String task, ModelSearchResult model);

  /**
   * Subclasses implement this to perform the actual provider API call.
   */
  protected abstract JsonNode doExecute(String owner, String modelId, JsonNode input);

  /**
   * Subclasses implement this to extract the final results (URL, text, etc.) from provider output.
   */
  protected abstract List<String> extractOutputs(JsonNode output);

  /**
   * Orchestrates the full pipeline: plan → transform input → execute (with retry) → extract →
   * download/store → finalize artifacts.
   */
  @Override
  public ExecutionResult run(List<Artifact> artifacts, String task, ModelSearchResult model,
      UnaryOperator<JsonNode> inputTransformer) {
    var plan = generatePlan(task, model);
    if (plan.hasError()) {
      throw new RuntimeException("Planning failed: " + plan.errorMessage());
    }

    var finalInput = inputTransformer.apply(plan.providerInput());
    var output = retryTemplate.execute(
        context -> doExecute(model.owner(), model.modelId(), finalInput));
    var extractedOutputs = extractOutputs(output);

    if (extractedOutputs == null || extractedOutputs.isEmpty() || "null".equals(
        extractedOutputs.get(0))) {
      throw new IllegalStateException("Model returned no usable output");
    }

    if (extractedOutputs.size() != artifacts.size()) {
      throw new IllegalStateException(
          "Model returned " + extractedOutputs.size() + " outputs, but " + artifacts.size()
              + " were expected.");
    }

    List<Artifact> finalizedArtifacts = new java.util.ArrayList<>();
    for (int i = 0; i < artifacts.size(); i++) {
      finalizedArtifacts.add(finalizeArtifact(artifacts.get(i), extractedOutputs.get(i)));
    }

    return new ExecutionResult(finalizedArtifacts, finalInput);
  }

  /**
   * Finalize the artifact by storing its content. For TEXT artifacts, sets inline content. For
   * others, downloads from the provider URL and stores in the storage service.
   */
  protected Artifact finalizeArtifact(Artifact artifact, String extractedOutput) {
    if (artifact.kind() == ArtifactKind.TEXT) {
      return artifact
          .withInlineContent(new TextNode(extractedOutput))
          .withStatus(ArtifactStatus.MANIFESTED)
          .withMimeType("text/plain");
    }

    // For non-text: download from URL and store
    return downloadAndStore(artifact, extractedOutput);
  }

  /**
   * Download content from a provider URL and store it via StorageService.
   */
  private Artifact downloadAndStore(Artifact artifact, String providerUrl) {
    final int maxAttempts = 3;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      try {
        URL url = new URI(providerUrl).toURL();
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(30000);

        try (var stream = connection.getInputStream()) {
          byte[] data = stream.readAllBytes();
          long contentLength = connection.getContentLengthLong();
          if (contentLength <= 0) {
            contentLength = data.length;
          }

          String mimeType = detectMimeType(connection, providerUrl, artifact.kind());
          String storageKey = storageService.store(data, mimeType);

          // Update metadata with contentLength
          JsonNode metadata = artifact.metadata();
          if (metadata == null || metadata.isNull()) {
            metadata = objectMapper.createObjectNode();
          }
          if (metadata.isObject()) {
            ((ObjectNode) metadata).put("contentLength", contentLength);
          }

          log.info("Stored artifact {} (kind: {}, MIME: {}, key: {})",
              artifact.name(), artifact.kind(), mimeType, storageKey);

          return artifact
              .withStorageKey(storageKey)
              .withMimeType(mimeType)
              .withMetadata(metadata)
              .withStatus(ArtifactStatus.MANIFESTED);
        }
      } catch (Exception e) {
        log.warn("Download attempt {} failed for artifact {}: {}", attempt + 1, artifact.name(),
            e.getMessage());
        if (attempt < maxAttempts - 1) {
          try {
            Thread.sleep(500L * (attempt + 1));
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    String errorMsg = "Failed to download and store after " + maxAttempts + " attempts";
    log.error("{}: artifact {}, url {}", errorMsg, artifact.name(), providerUrl);
    return artifact
        .withStatus(ArtifactStatus.FAILED)
        .withErrorMessage(errorMsg);
  }

  private String detectMimeType(URLConnection connection, String url, ArtifactKind kind) {
    // 1. Get MIME type from Response Header
    String mimeType = connection.getContentType();

    // 2. Fallback to URL-based guessing if header is generic or missing
    if (mimeType == null || mimeType.startsWith("application/octet-stream")) {
      mimeType = URLConnection.guessContentTypeFromName(url);
    }

    // 3. Final fallback based on ArtifactKind
    if (mimeType == null) {
      mimeType = switch (kind) {
        case IMAGE -> "image/png";
        case AUDIO -> "audio/mpeg";
        case VIDEO -> "video/mp4";
        case PDF -> "application/pdf";
        case TEXT -> "text/plain";
        default -> "application/octet-stream";
      };
    }

    // Clean up MIME type (remove charset, etc.)
    if (mimeType.contains(";")) {
      mimeType = mimeType.split(";")[0].trim();
    }

    return mimeType;
  }
}
