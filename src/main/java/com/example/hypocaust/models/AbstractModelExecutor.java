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
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
public abstract class AbstractModelExecutor implements ModelExecutor {

  private static final AnthropicChatModelSpec PROMPT_ENG_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;
  private static final int MAX_RETRIES = 2;
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

  /**
   * Subclasses implement this to perform the actual provider API call.
   */
  protected abstract JsonNode doExecute(String owner, String modelId, JsonNode input);

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

  /**
   * Executes with automatic retry on transient failures. Returns output on success; throws on
   * permanent or exhausted-retry failure. Callers never need to know retries happened.
   */
  @Override
  public final JsonNode execute(String owner, String modelId, JsonNode input) {
    for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
      try {
        return doExecute(owner, modelId, input);
      } catch (Exception e) {
        if (!isTransient(e) || attempt == MAX_RETRIES) {
          log.warn("[{}] {} for {}/{}: {}",
              platform(), attempt > 0 ? "Exhausted " + (attempt + 1) + " attempts" : "Failed",
              owner, modelId, e.getMessage());
          throw e;
        }
        log.info("[{}] Transient error (attempt {}/{}), retrying in {}ms: {}",
            platform(), attempt + 1, MAX_RETRIES + 1, BACKOFF_MS[attempt], e.getMessage());
        sleep(BACKOFF_MS[attempt]);
      }
    }
    throw new IllegalStateException("Unreachable");
  }

  /**
   * Classifies whether an exception represents a transient failure worth retrying.
   */
  public static boolean isTransient(Throwable e) {
    if (e instanceof ResourceAccessException) {
      return true;
    }
    if (e instanceof HttpServerErrorException serverError) {
      int code = serverError.getStatusCode().value();
      return code == 502 || code == 503 || code == 504;
    }
    if (e instanceof HttpClientErrorException clientError) {
      return clientError.getStatusCode().value() == 429;
    }
    // Walk the full cause chain for underlying connection issues
    Throwable cause = e;
    while ((cause = cause.getCause()) != null) {
      if (cause instanceof ConnectException || cause instanceof SocketTimeoutException) {
        return true;
      }
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
