package com.example.hypocaust.models.replicate;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReplicateModelExecutor extends AbstractModelExecutor {

  private final ReplicateClient replicateClient;

  public ReplicateModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, ReplicateClient replicateClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate);
    this.replicateClient = replicateClient;
  }

  @Override
  public Platform platform() {
    return Platform.REPLICATE;
  }

  @Override
  public ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    String schemaContext;
    try {
      var version = replicateClient.getLatestVersion(owner, modelId);
      var fullSchema = replicateClient.getSchema(owner, modelId, version);
      var inputSchema = fullSchema.path("components").path("schemas").path("Input");
      if (inputSchema.isMissingNode()) {
        inputSchema = fullSchema;
      }
      schemaContext = "Schema: " + inputSchema;
    } catch (Exception e) {
      log.warn("Failed to fetch Replicate schema for {}/{}: {}", owner, modelId, e.getMessage());
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
        Kind: %s
        Model Docs: %s
        %s
        
        Best Practices:
        %s
        """, task, kind, description, schemaContext, bestPractices);

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
  public String extractOutput(JsonNode output) {
    if (output.isTextual()) {
      return output.asText();
    }
    if (output.isArray() && !output.isEmpty()) {
      String first = output.get(0).asText();
      if (isUrl(first)) {
        return first; // Take the first generated image
      } else {
        // Join tokens for LLM/Text models
        StringBuilder sb = new StringBuilder();
        output.forEach(node -> sb.append(node.asText()));
        return sb.toString();
      }
    }
    if (output.has("url")) {
      return output.get("url").asText();
    }
    return output.toString();
  }

  private boolean isUrl(String s) {
    return s != null && (s.startsWith("http://") || s.startsWith("https://"));
  }
}
