package com.example.hypocaust.models.openrouter;

import com.example.hypocaust.models.AbstractModelExecutor;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExtractedOutput;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.Platform;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.openrouter.api-key")
@Slf4j
public class OpenRouterModelExecutor extends AbstractModelExecutor {

  private final OpenRouterClient openRouterClient;

  public OpenRouterModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate, StorageService storageService,
      OpenRouterClient openRouterClient) {
    super(modelRegistry, objectMapper, chatService, retryTemplate, storageService);
    this.openRouterClient = openRouterClient;
  }

  @Override
  public Platform platform() {
    return Platform.OPENROUTER;
  }

  @Override
  protected ExecutionPlan generatePlan(String task, ModelSearchResult model) {
    return new ExecutionPlan(objectMapper.createObjectNode().put("prompt", task), null);
  }

  @Override
  protected JsonNode doExecute(String owner, String modelId, JsonNode input) {
    var fullModelId = owner + "/" + modelId;
    return openRouterClient.chatCompletion(fullModelId, input);
  }

  @Override
  protected List<ExtractedOutput> extractOutputs(JsonNode output) {
    if (output.has("choices") && output.get("choices").isArray()
        && !output.get("choices").isEmpty()) {
      return List.of(ExtractedOutput.ofContent(
          output.get("choices").get(0).path("message").path("content").asText()));
    }
    return List.of(ExtractedOutput.ofContent(output.toString()));
  }
}
