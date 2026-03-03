package com.example.hypocaust.models.replicate;

import com.example.hypocaust.domain.IntentMapping;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReplicateModelExecutor extends AbstractModelExecutor {

  private final ReplicateClient replicateClient;

  public ReplicateModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ArtifactResolver artifactResolver, ReplicateClient replicateClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService,
        artifactResolver);
    this.replicateClient = replicateClient;
  }

  @Override
  public Platform platform() {
    return Platform.REPLICATE;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<IntentMapping> intents) {
    String schemaContext;
    try {
      var version = replicateClient.getLatestVersion(model.owner(), model.modelId());
      var fullSchema = replicateClient.getSchema(model.owner(), model.modelId(), version);
      var inputSchema = fullSchema.path("components").path("schemas").path("Input");
      if (inputSchema.isMissingNode()) {
        inputSchema = fullSchema;
      }
      schemaContext = "Schema: " + inputSchema;
    } catch (Exception e) {
      log.warn("Failed to fetch Replicate schema for {}/{}: {}", model.owner(), model.modelId(),
          e.getMessage());
      schemaContext = "Schema: unavailable";
    }

    var systemPrompt = PromptBuilder.create()
        .with(new PromptFragment("replicate-plan", """
            You are an expert creative director. Prepare a Replicate generation plan.

            YOUR RESPONSIBILITIES:
            1. Input Mapping: Construct the 'providerInput' matching the OpenAPI schema.
               - Optimize prompts for the best artistic results.
               - Map user requirements to specific schema fields.
               - If a field requires a URL and the user refers to an artifact, use '@artifact_name'.
            2. Validation:
               - Ensure all REQUIRED fields are present.
               - If mandatory info is missing, provide a precise 'errorMessage'.

            OUTPUT: Return ONLY valid JSON:
            {
              "providerInput": { ... },
              "errorMessage": null or "..."
            }
            """))
        .with(PromptFragments.abilityAwareness())
        .build();

    var userPrompt = String.format("""
        Task: %s
        Model Docs: %s
        %s

        Best Practices:
        %s
        """, task, model.description(), schemaContext, model.bestPractices());

    var response = chatService.call(PROMPT_ENG_MODEL, systemPrompt, userPrompt);
    try {
      var node = objectMapper.readTree(
          com.example.hypocaust.common.JsonUtils.extractJson(response));
      return new ExecutionPlan(
          node.path("providerInput"),
          node.path("errorMessage").isTextual() ? node.path("errorMessage").asText() : null
      );
    } catch (Exception e) {
      return ExecutionPlan.error("Plan generation failed: " + e.getMessage());
    }
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    var version = replicateClient.getLatestVersion(owner, modelId);
    return replicateClient.predict(owner, modelId, version, input);
  }

  @Override
  protected List<ExtractedOutput> extractOutputs(JsonNode output) {
    if (output.isTextual()) {
      return List.of(ExtractedOutput.ofContent(output.asText()));
    }
    if (output.isArray() && !output.isEmpty()) {
      String first = output.get(0).asText();
      if (isUrl(first)) {
        return List.of(ExtractedOutput.ofContent(first));
      } else {
        StringBuilder sb = new StringBuilder();
        output.forEach(node -> sb.append(node.asText()));
        return List.of(ExtractedOutput.ofContent(sb.toString()));
      }
    }
    if (output.has("url")) {
      return List.of(ExtractedOutput.ofContent(output.get("url").asText()));
    }
    return List.of(ExtractedOutput.ofContent(output.toString()));
  }

  private boolean isUrl(String s) {
    return s != null && (s.startsWith("http://") || s.startsWith("https://"));
  }
}
