package com.example.hypocaust.models;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  protected AbstractModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper,
      ChatService chatService, RetryTemplate retryTemplate) {
    this.modelRegistry = modelRegistry;
    this.objectMapper = objectMapper;
    this.chatService = chatService;
    this.retryTemplate = retryTemplate;
  }

  /**
   * Subclasses implement this to prepare provider-specific input from the user task.
   */
  protected abstract ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices);

  /**
   * Subclasses implement this to perform the actual provider API call.
   */
  protected abstract JsonNode doExecute(String owner, String modelId, JsonNode input);

  /**
   * Subclasses implement this to extract the final result (URL, text, etc.) from provider output.
   */
  protected abstract String extractOutput(JsonNode output);

  /**
   * Orchestrates the full pipeline: plan → transform input → execute (with retry) → extract.
   */
  @Override
  public ExecutionResult run(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices,
      UnaryOperator<JsonNode> inputTransformer) {
    var plan = generatePlan(task, kind, modelName, owner, modelId, description, bestPractices);
    if (plan.hasError()) {
      throw new RuntimeException("Planning failed: " + plan.errorMessage());
    }

    var finalInput = inputTransformer.apply(plan.providerInput());
    var output = retryTemplate.execute(context -> doExecute(owner, modelId, finalInput));
    var result = extractOutput(output);
    return new ExecutionResult(result, finalInput);
  }
}
