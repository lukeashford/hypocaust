package com.example.hypocaust.models;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptBuilder;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
public abstract class AbstractModelExecutor implements ModelExecutor {

  private static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_TRANSIENT_RETRIES = 2;
  private static final long[] BACKOFF_MS = {1_000, 3_000};

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
      String owner, String modelId, String description, String bestPractices) {
    try {
      var chatClient = ChatClient.builder(modelRegistry.get(PROMPT_ENG_MODEL)).build();

      var additionalContext = additionalPlanContext(owner, modelId, description, bestPractices);

      var systemPrompt = PromptBuilder.create()
          .with(PromptFragments.planSystemPrompt())
          .with(new PromptFragment(platform().name().toLowerCase() + "-plan", planSystemPrompt()))
          .with(PromptFragments.abilityAwareness())
          .build();

      var response = chatClient.prompt()
          .system(systemPrompt)
          .user(String.format("""
              Task: %s
              Kind: %s
              %s
              """, task, kind, additionalContext))
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

  @Override
  public ExecutionAttempt executeWithRetry(String owner, String modelId, JsonNode input) {
    List<Map<String, String>> attempts = new ArrayList<>();

    for (int attempt = 0; attempt <= MAX_TRANSIENT_RETRIES; attempt++) {
      try {
        var output = execute(owner, modelId, input);

        var meta = new LinkedHashMap<String, String>();
        meta.put("attempt", String.valueOf(attempt + 1));
        meta.put("platform", platform().name());
        meta.put("model", owner + "/" + modelId);
        meta.put("status", "success");
        attempts.add(meta);

        return ExecutionAttempt.success(output, attempts);
      } catch (Exception e) {
        var meta = new LinkedHashMap<String, String>();
        meta.put("attempt", String.valueOf(attempt + 1));
        meta.put("platform", platform().name());
        meta.put("model", owner + "/" + modelId);
        meta.put("status", "failed");
        meta.put("error", e.getMessage());
        meta.put("errorType", e.getClass().getSimpleName());
        meta.put("transient", String.valueOf(isTransient(e)));
        attempts.add(meta);

        if (!isTransient(e) || attempt == MAX_TRANSIENT_RETRIES) {
          log.warn("[{}] {} failure for {}/{}: {}",
              platform(), isTransient(e) ? "Final transient" : "Non-transient",
              owner, modelId, e.getMessage());
          return ExecutionAttempt.failure(attempts);
        }

        long delay = BACKOFF_MS[attempt];
        log.info("[{}] Transient error on attempt {} for {}/{}, retrying in {}ms: {}",
            platform(), attempt + 1, owner, modelId, delay, e.getMessage());
        sleep(delay);
      }
    }

    return ExecutionAttempt.failure(attempts);
  }

  /**
   * Determines whether an exception is likely transient (network issue, server overload) and worth
   * retrying, vs. a permanent failure (bad request, model not found) that should fail immediately.
   */
  static boolean isTransient(Throwable e) {
    if (e instanceof ResourceAccessException) {
      return true;
    }
    if (e instanceof HttpServerErrorException serverError) {
      int code = serverError.getStatusCode().value();
      return code == 502 || code == 503 || code == 504 || code == 429;
    }
    Throwable cause = e.getCause();
    if (cause instanceof ConnectException || cause instanceof SocketTimeoutException) {
      return true;
    }
    String msg = e.getMessage();
    if (msg != null) {
      String lower = msg.toLowerCase();
      if (lower.contains("timed out") || lower.contains("connection refused")
          || lower.contains("service unavailable") || lower.contains("rate limit")
          || lower.contains("too many requests")) {
        return true;
      }
    }
    return false;
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }
}
