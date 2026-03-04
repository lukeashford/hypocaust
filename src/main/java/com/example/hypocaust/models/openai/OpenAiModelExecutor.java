package com.example.hypocaust.models.openai;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenAiModelExecutor extends AbstractModelExecutor {

  public OpenAiModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      ArtifactResolver artifactResolver) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService,
        artifactResolver);
  }

  @Override
  public Platform platform() {
    return Platform.OPENAI;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model,
      List<Artifact> artifacts) {
    if (artifacts.size() != 1) {
      return ExecutionPlan.error(
          "OpenAI chat models produce exactly 1 text output per call, but "
              + artifacts.size() + " artifacts were expected. "
              + "Consider generating them individually in separate calls.");
    }
    var artifact = artifacts.getFirst();
    if (artifact.kind() != ArtifactKind.TEXT) {
      return ExecutionPlan.error(
          "OpenAI chat models support only TEXT output, but received "
              + artifact.kind() + " artifact '" + artifact.name() + "': " + artifact.description()
              + ". Choose a different model for " + artifact.kind() + " generation.");
    }
    return new ExecutionPlan(
        objectMapper.createObjectNode().put("prompt", task),
        Map.of("text", artifact.name()),
        null);
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    var chatModel = modelRegistry.get(modelId);
    String prompt = input.path("prompt").asText();

    var response = ChatClient.builder(chatModel)
        .build()
        .prompt(prompt)
        .call()
        .content();

    return objectMapper.valueToTree(Map.of("content", response));
  }

  @Override
  protected Map<String, ExtractedOutput> extractOutputs(JsonNode output) {
    return Map.of("text", ExtractedOutput.ofContent(output.path("content").asText()));
  }
}
