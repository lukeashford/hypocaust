package com.example.hypocaust.models.replicate;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.util.ArtifactResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

  private static final String REPLICATE_SYSTEM_PROMPT = """
      You are planning for a Replicate model.

      INPUT MAPPING:
      - Construct the 'providerInput' matching the OpenAPI schema provided below.
      - Optimize prompts for the best artistic results.
      - Map user requirements to specific schema fields.

      VALIDATION:
      - Ensure all REQUIRED fields are present.
      - If mandatory info is missing, provide a precise 'errorMessage'.

      OUTPUT KEY CONVENTIONS for outputMapping:
      - Most Replicate models produce a single output. Use "output" as the key.
      """;

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts) {
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

    return generatePlanWithLlm(task, model, artifacts, REPLICATE_SYSTEM_PROMPT, schemaContext);
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    var version = replicateClient.getLatestVersion(owner, modelId);
    return replicateClient.predict(owner, modelId, version, input);
  }

  @Override
  protected Map<String, ExtractedOutput> extractOutputs(JsonNode output) {
    if (output.isTextual()) {
      return Map.of("output", ExtractedOutput.ofContent(output.asText()));
    }
    if (output.isArray() && !output.isEmpty()) {
      String first = output.get(0).asText();
      if (isUrl(first)) {
        return Map.of("output", ExtractedOutput.ofContent(first));
      } else {
        StringBuilder sb = new StringBuilder();
        output.forEach(node -> sb.append(node.asText()));
        return Map.of("output", ExtractedOutput.ofContent(sb.toString()));
      }
    }
    if (output.has("url")) {
      return Map.of("output", ExtractedOutput.ofContent(output.get("url").asText()));
    }
    return Map.of("output", ExtractedOutput.ofContent(output.toString()));
  }

  private boolean isUrl(String s) {
    return s != null && (s.startsWith("http://") || s.startsWith("https://"));
  }
}
