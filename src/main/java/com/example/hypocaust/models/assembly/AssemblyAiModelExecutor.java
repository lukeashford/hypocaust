package com.example.hypocaust.models.assembly;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.assemblyai.api-key")
@Slf4j
public class AssemblyAiModelExecutor extends AbstractModelExecutor {

  private final AssemblyAiClient assemblyAiClient;

  public AssemblyAiModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ArtifactResolver artifactResolver, AssemblyAiClient assemblyAiClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService,
        artifactResolver);
    this.assemblyAiClient = assemblyAiClient;
  }

  @Override
  public Platform platform() {
    return Platform.ASSEMBLYAI;
  }

  private static final String ASSEMBLYAI_SYSTEM_PROMPT = """
      You are planning for an AssemblyAI audio processing model.

      INPUT MAPPING:
      - Construct the 'providerInput' object following the model's input spec described in the
        Model Docs and Best Practices below.
      - If a field requires an audio URL and the user refers to an artifact, use '@artifact_name'.

      VALIDATION:
      - If mandatory audio source is missing, provide an 'errorMessage'.

      OUTPUT KEY CONVENTIONS for outputMapping:
      - Use "transcript" as the output key for transcription results.
      """;

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts) {
    return generatePlanWithLlm(task, model, artifacts, ASSEMBLYAI_SYSTEM_PROMPT, null);
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    return switch (modelId) {
      case "transcript" -> assemblyAiClient.transcribe(input);
      case "audio-intelligence" -> assemblyAiClient.transcribeWithIntelligence(input);
      default -> {
        log.warn("Unknown AssemblyAI model ID: {}, falling back to transcription", modelId);
        yield assemblyAiClient.transcribe(input);
      }
    };
  }

  @Override
  protected Map<String, ExtractedOutput> extractOutputs(JsonNode output) {
    if (output.has("text") && output.get("text").isTextual()) {
      return Map.of("transcript", ExtractedOutput.ofContent(output.get("text").asText()));
    }
    if (output.has("id")) {
      return Map.of("transcript", ExtractedOutput.ofContent(output.get("id").asText()));
    }
    if (output.has("chapters")) {
      return Map.of("transcript", ExtractedOutput.ofContent(output.toString()));
    }
    return Map.of("transcript", ExtractedOutput.ofContent(output.toString()));
  }
}
