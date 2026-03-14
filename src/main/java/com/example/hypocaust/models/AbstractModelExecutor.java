package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;

/**
 * Base executor implementing the full pipeline: plan → resolve artifact refs → execute phases (each
 * with retry) → extract → download/store → finalize artifacts.
 *
 * <p>Subclasses implement {@link #generatePlan} to produce an {@link ExecutionPlan} containing the
 * provider input and an {@link ExecutionPlan#outputMapping()} that maps provider output keys to
 * artifact names. Simple executors (text-only LLMs) build this deterministically; complex executors
 * delegate to {@link #generatePlanWithLlm} which provides a common prompt structure ensuring
 * consistent output mapping across all LLM-planned providers.
 *
 * <p><b>Error message contract for decomposer interpretability:</b>
 * Every exception message thrown from {@link #run} should be self-classifying:
 * <ol>
 *   <li>State what went wrong (the fact)</li>
 *   <li>State why it went wrong (infrastructure, bad input, or capacity limit)</li>
 *   <li>Suggest what to try instead (if applicable)</li>
 * </ol>
 * <p>Example: "This model produces 1 output(s) per call, but 3 were expected.
 * Consider generating them individually in separate calls."
 *
 * <p>The decomposer LLM classifies errors by understanding the message,
 * not by keyword matching. No error tags or category enums are needed.
 */
@Slf4j
public abstract class AbstractModelExecutor implements ModelExecutor {

  protected static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  protected final ModelRegistry modelRegistry;
  protected final ObjectMapper objectMapper;
  protected final ChatService chatService;
  protected final RetryTemplate retryTemplate;
  protected final StorageService storageService;
  protected final ArtifactResolver artifactResolver;

  protected AbstractModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ArtifactResolver artifactResolver) {
    this.modelRegistry = modelRegistry;
    this.objectMapper = objectMapper;
    this.chatService = chatService;
    this.retryTemplate = retryTemplate;
    this.storageService = storageService;
    this.artifactResolver = artifactResolver;
  }

  // ---------------------------------------------------------------------------
  // Planning
  // ---------------------------------------------------------------------------

  /**
   * Subclasses implement this to produce an {@link ExecutionPlan} for the given task and gestating
   * artifacts. Simple executors build the plan deterministically; complex ones delegate to
   * {@link #generatePlanWithLlm}.
   *
   * @param task the user task description
   * @param model the consolidated model context from RAG
   * @param artifacts the GESTATING artifacts that must be fulfilled
   */
  protected abstract ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts);

  /**
   * Shared LLM-based planning for executors that need prompt engineering to construct provider
   * input. Builds a common system prompt (artifact expectations, output mapping format, error
   * contract) and appends the provider-specific system prompt. The LLM response is parsed into an
   * {@link ExecutionPlan} with the output mapping.
   *
   * @param task the user task description
   * @param model the consolidated model context from RAG
   * @param artifacts the GESTATING artifacts to fulfil
   * @param providerSystemPrompt provider-specific planning instructions (appended to the common
   * system prompt)
   * @param additionalUserContext optional extra context for the user prompt (e.g., a fetched API
   * schema); may be null
   */
  protected ExecutionPlan generatePlanWithLlm(String task, ModelSearchResult model,
      List<Artifact> artifacts, String providerSystemPrompt, String additionalUserContext) {

    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("executor-plan-common", """
            You are an expert creative director. Prepare a generation plan that maps a user's
            task to a provider API call and connects the provider's outputs to the expected
            artifacts.
            
            ## Expected artifacts
            
            You are given a list of artifacts that MUST be fulfilled by this generation.
            Each artifact has a name, kind, and description.
            
            ## Your responsibilities
            
            1. **providerInput** — Construct the JSON object to send to the provider API,
               following the model docs and best practices below.
               - If a field requires a URL and the user refers to an artifact, use
                 '@artifact_name' as a placeholder. The system resolves these automatically.
               - To reference metadata from an existing artifact, use
                 '@artifact_name.metadata.fieldName' (e.g., '@voice_sample.metadata.voiceId').
            
            2. **outputMapping** — Map each expected provider output to exactly one artifact
               name. The keys are stable identifiers you choose for each distinct output the
               provider will produce (e.g., "image", "audio", "preview_0", "preview_1").
               The values are artifact names from the list below. Every artifact name MUST
               appear exactly once as a value. You may include extra output keys that do not
               map to any artifact — they will be silently ignored.
            
            3. **errorMessage** — If you cannot fulfil the request (missing input, incompatible
               model, unsupported kind), set this to a clear message explaining what is wrong
               and what to try instead. Set to null otherwise.
            
            ## Output format
            
            Return ONLY valid JSON:
            {
              "providerInput": { ... },
              "outputMapping": { "<outputKey>": "<artifactName>", ... },
              "errorMessage": null or "..."
            }
            """))
        .with(new PromptFragment("executor-plan-provider", providerSystemPrompt))
        .with(PromptFragments.abilityAwareness())
        .build();

    var userPrompt = new StringBuilder();
    userPrompt.append("Task: ").append(task).append("\n\n");
    userPrompt.append(formatArtifactContext(artifacts)).append("\n");
    userPrompt.append("Model: ").append(model.name())
        .append(" (id: ").append(model.modelId()).append(")\n");
    if (model.optionalInputs() != null && !model.optionalInputs().isEmpty()) {
      userPrompt.append("Model Optional Inputs: ").append(model.optionalInputs())
          .append(" — include in providerInput if matching input artifacts are available.\n");
    }
    userPrompt.append("Model Docs: ").append(model.description()).append("\n\n");
    userPrompt.append("Best Practices:\n").append(model.bestPractices()).append("\n");
    if (additionalUserContext != null && !additionalUserContext.isBlank()) {
      userPrompt.append("\n").append(additionalUserContext).append("\n");
    }

    var response = chatService.call(PROMPT_ENG_MODEL, systemPrompt, userPrompt.toString(),
        String.class);
    try {
      var node = objectMapper.readTree(
          com.example.hypocaust.common.JsonUtils.extractJson(response));

      // Parse outputMapping
      Map<String, String> outputMapping = new LinkedHashMap<>();
      var mappingNode = node.path("outputMapping");
      if (mappingNode.isObject()) {
        mappingNode.fields().forEachRemaining(
            entry -> outputMapping.put(entry.getKey(), entry.getValue().asText()));
      }

      String errorMessage = node.path("errorMessage").isTextual()
          ? node.path("errorMessage").asText() : null;

      return new ExecutionPlan(node.path("providerInput"), outputMapping, errorMessage);
    } catch (Exception e) {
      return ExecutionPlan.error("Plan generation failed: " + e.getMessage());
    }
  }

  /**
   * Formats the gestating artifacts into a human-readable context block for inclusion in planning
   * prompts. Replaces the old intent-based formatting with artifact-based context.
   */
  protected String formatArtifactContext(List<Artifact> artifacts) {
    if (artifacts == null || artifacts.isEmpty()) {
      return "";
    }
    var sb = new StringBuilder("Expected artifacts to fulfil:\n");
    for (int i = 0; i < artifacts.size(); i++) {
      var artifact = artifacts.get(i);
      sb.append("  [").append(i + 1).append("] ")
          .append(artifact.kind()).append(" \"").append(artifact.name()).append("\"");
      sb.append(": ").append(artifact.description()).append("\n");
    }
    return sb.toString();
  }

  // ---------------------------------------------------------------------------
  // Execution
  // ---------------------------------------------------------------------------

  /**
   * Subclasses implement this to perform the actual provider API call.
   */
  protected abstract JsonNode doExecute(String owner, String modelId, JsonNode input);

  /**
   * Subclasses implement this to extract the results from a provider response. Returns a map from
   * output keys to extracted outputs. The keys must match those used in the
   * {@link ExecutionPlan#outputMapping()} so the orchestrator can route each output to the correct
   * artifact. Outputs whose keys are not present in the mapping are silently ignored.
   */
  protected abstract Map<String, ExtractedOutput> extractOutputs(JsonNode output);

  /**
   * A single step in the execution pipeline. Each phase receives the original (resolved) input and
   * the previous phase's output, allowing multi-step flows where later phases use earlier results.
   */
  @FunctionalInterface
  protected interface ExecutionPhase {

    JsonNode execute(JsonNode originalInput, JsonNode previousOutput);
  }

  /**
   * Build the execution phases for this request. Default: a single phase wrapping
   * {@link #doExecute}. Subclasses override to define multi-step flows (e.g., voice design → save →
   * TTS) where each phase gets its own retry.
   */
  protected List<ExecutionPhase> buildExecutionPhases(String owner, String modelId,
      JsonNode input) {
    return List.of((originalInput, previousOutput) -> doExecute(owner, modelId, originalInput));
  }

  // ---------------------------------------------------------------------------
  // Orchestration
  // ---------------------------------------------------------------------------

  /**
   * Orchestrates the full pipeline: plan → resolve artifact refs → execute phases (each with retry)
   * → extract → map outputs to artifacts via outputMapping → download/store → finalize.
   */
  @Override
  public ExecutionResult run(List<Artifact> artifacts, String task, ModelSearchResult model,
      List<Artifact> availableArtifacts) {
    var plan = generatePlan(task, model, artifacts);
    if (plan.hasError()) {
      throw new RuntimeException("Planning failed: " + plan.errorMessage());
    }

    // Validate that every gestating artifact appears in the output mapping
    var mappedArtifactNames = new java.util.HashSet<>(plan.outputMapping().values());
    for (var artifact : artifacts) {
      if (!mappedArtifactNames.contains(artifact.name())) {
        throw new IllegalStateException(
            "Planning produced no output mapping for artifact '" + artifact.name()
                + "'. The plan must map at least one output key to every expected artifact.");
      }
    }

    // Resolve artifact placeholders internally
    var finalInput = artifactResolver.resolve(plan.providerInput(), availableArtifacts);

    // Execute all phases sequentially, each with its own retry
    var phases = buildExecutionPhases(model.owner(), model.modelId(), finalInput);
    JsonNode output = null;
    for (var phase : phases) {
      final var previousOutput = output;
      output = retryTemplate.execute(
          context -> phase.execute(finalInput, previousOutput));
    }

    var extractedOutputs = extractOutputs(output);

    if (extractedOutputs == null || extractedOutputs.isEmpty()) {
      throw new IllegalStateException("Model returned no usable output");
    }

    // Build reverse map: artifactName → outputKey
    Map<String, String> artifactNameToOutputKey = new HashMap<>();
    for (var entry : plan.outputMapping().entrySet()) {
      artifactNameToOutputKey.put(entry.getValue(), entry.getKey());
    }

    List<Artifact> finalizedArtifacts = new ArrayList<>();
    for (var artifact : artifacts) {
      String outputKey = artifactNameToOutputKey.get(artifact.name());
      var extracted = extractedOutputs.get(outputKey);
      if (extracted == null || "null".equals(extracted.content())) {
        throw new IllegalStateException(
            "Model produced no output for key '" + outputKey
                + "' (expected for artifact '" + artifact.name() + "'). "
                + "Available output keys: " + extractedOutputs.keySet());
      }

      var finalized = finalizeArtifact(artifact, extracted.content());

      // Merge executor-provided metadata (e.g., voiceId) into the artifact
      if (extracted.metadata() != null && !extracted.metadata().isEmpty()) {
        JsonNode existingMeta = finalized.metadata();
        if (existingMeta == null || existingMeta.isNull()) {
          existingMeta = objectMapper.createObjectNode();
        }
        if (existingMeta.isObject()) {
          ((ObjectNode) existingMeta).setAll((ObjectNode) extracted.metadata());
        }
        finalized = finalized.toBuilder().metadata(existingMeta).build();
      }

      finalizedArtifacts.add(finalized);
    }

    return new ExecutionResult(finalizedArtifacts, finalInput);
  }

  // ---------------------------------------------------------------------------
  // Artifact finalization
  // ---------------------------------------------------------------------------

  /**
   * Finalize the artifact by storing its content. For TEXT artifacts, sets inline content. For
   * others, downloads from the provider URL and stores in the storage service.
   */
  protected Artifact finalizeArtifact(Artifact artifact, String extractedOutput) {
    if (artifact.kind() == ArtifactKind.TEXT) {
      return artifact.toBuilder()
          .inlineContent(new TextNode(extractedOutput))
          .status(ArtifactStatus.MANIFESTED)
          .mimeType("text/plain")
          .build();
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

          return artifact.toBuilder()
              .storageKey(storageKey)
              .mimeType(mimeType)
              .metadata(metadata)
              .status(ArtifactStatus.MANIFESTED)
              .build();
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
    return artifact.toBuilder()
        .status(ArtifactStatus.FAILED)
        .errorMessage(errorMsg)
        .build();
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
