package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Base class for LLM-powered name generation with retry, sanitization, and fallback logic. Both
 * artifact names and task execution names share this infrastructure.
 */
@Slf4j
public abstract class LlmNameGeneratorService {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;
  private static final int MAX_RETRIES = 3;

  private final ModelRegistry modelRegistry;

  protected LlmNameGeneratorService(ModelRegistry modelRegistry) {
    this.modelRegistry = modelRegistry;
  }

  protected abstract String systemPrompt();

  protected abstract int maxLength();

  /**
   * Core generation logic: try LLM up to {@link #MAX_RETRIES} times, sanitize responses, fall back
   * to sanitized input with a counter suffix if all attempts fail.
   *
   * @param userPrompt the prompt to send to the LLM
   * @param fallbackInput raw text to sanitize as a last resort
   * @param existingNames names already taken (for uniqueness checks)
   * @return a unique name
   */
  protected String generateUniqueName(String userPrompt, String fallbackInput,
      Set<String> existingNames) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(MODEL)).build();

      for (int i = 0; i < MAX_RETRIES; i++) {
        String response = chatClient.prompt()
            .system(systemPrompt())
            .user(userPrompt)
            .call()
            .content();

        if (response != null && !response.isBlank()) {
          String name = sanitize(response);
          if (!existingNames.contains(name)) {
            return name;
          }
          log.info("LLM generated an existing name: {}. Attempt {}/{}", name, i + 1, MAX_RETRIES);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to generate name via LLM: {}", e.getMessage());
    }

    // Fallback: sanitize the raw input and append a counter if needed
    String name = sanitize(fallbackInput);
    if (name.length() > maxLength()) {
      name = name.substring(0, maxLength());
    }
    return appendCounterIfExists(name, existingNames);
  }

  protected String sanitize(String input) {
    return input.toLowerCase()
        .replaceAll("[^a-z0-9_]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");
  }

  private String appendCounterIfExists(String name, Set<String> existingNames) {
    String result = name;
    int counter = 2;
    while (existingNames.contains(result)) {
      result = name + "_" + counter;
      counter++;
    }
    return result;
  }
}
