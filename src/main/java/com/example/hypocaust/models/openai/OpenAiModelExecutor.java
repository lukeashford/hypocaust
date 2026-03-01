package com.example.hypocaust.models.openai;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenAiModelExecutor extends AbstractModelExecutor {

  public OpenAiModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService);
  }

  @Override
  public Platform platform() {
    return Platform.OPENAI;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    return new ExecutionPlan(objectMapper.createObjectNode().put("prompt", task), null);
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
  protected String extractOutput(JsonNode output) {
    return output.path("content").asText();
  }
}
