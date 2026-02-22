package com.example.hypocaust.integration;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

@Slf4j
public abstract class AbstractModelExecutor implements ModelExecutor {

  private static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  protected final ModelRegistry modelRegistry;
  protected final ObjectMapper objectMapper;

  protected AbstractModelExecutor(ModelRegistry modelRegistry, ObjectMapper objectMapper) {
    this.modelRegistry = modelRegistry;
    this.objectMapper = objectMapper;
  }

  protected abstract String planSystemPrompt();

  protected abstract String additionalPlanContext(String owner, String modelId,
      String description, String bestPractices);

  @Override
  public ExecutionPlan generatePlan(String task, ArtifactKind kind, String modelName,
      String owner, String modelId, String description, String bestPractices,
      List<Artifact> availableArtifacts) {
    try {
      var chatClient = ChatClient.builder(modelRegistry.get(PROMPT_ENG_MODEL)).build();

      var artifactNames = availableArtifacts.stream().map(Artifact::name).toList();
      var additionalContext = additionalPlanContext(owner, modelId, description, bestPractices);

      var response = chatClient.prompt()
          .system(planSystemPrompt())
          .user(String.format("""
              Task: %s
              Kind: %s
              Artifacts: %s
              %s
              """, task, kind, artifactNames, additionalContext))
          .call()
          .content();

      var json = JsonUtils.extractJson(response);
      var node = objectMapper.readTree(json);
      return new ExecutionPlan(
          node.path("providerInput"),
          node.path("errorMessage").isTextual() ? node.path("errorMessage").asText() : null
      );
    } catch (Exception e) {
      log.error("Failed to generate plan for {}", platform(), e);
      return ExecutionPlan.error("Plan generation failed: " + e.getMessage());
    }
  }
}
