package com.example.hypocaust.models;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  protected AbstractModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate) {
    this.modelRegistry = modelRegistry;
    this.objectMapper = objectMapper;
    this.chatService = chatService;
    this.retryTemplate = retryTemplate;
  }

  /**
   * Subclasses implement this to perform the actual provider API call.
   */
  protected abstract JsonNode doExecute(String owner, String modelId, JsonNode input);

  @Override
  public JsonNode planAndExecute(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices) {
    var plan = generatePlan(task, kind, modelName, owner, modelId, description, bestPractices);
    if (plan.hasError()) {
      throw new RuntimeException("Planning failed: " + plan.errorMessage());
    }
    return execute(owner, modelId, plan.providerInput());
  }

  /**
   * Executes with automatic retry on transient failures. Returns output on success; throws on
   * permanent or exhausted-retry failure. Callers never need to know retries happened.
   */
  @Override
  public JsonNode execute(String owner, String modelId, JsonNode input) {
    return retryTemplate.execute(context -> doExecute(owner, modelId, input));
  }
}
