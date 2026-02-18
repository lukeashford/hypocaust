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
  protected final int maxLength;

  protected LlmNameGeneratorService(ModelRegistry modelRegistry, int maxLength) {
    this.modelRegistry = modelRegistry;
    this.maxLength = maxLength;
  }

  protected abstract String systemPrompt();

  protected abstract String defaultName();

  protected String buildSystemPrompt(String entityName, String additionalInstructions,
      String examples) {
    return String.format("""
            Generate a short snake_case name for %s (max %d chars).
            %sUse only lowercase letters, numbers, and underscores.
            Reply with ONLY the name, nothing else.
            Examples: %s
            """, entityName, maxLength,
        additionalInstructions != null ? additionalInstructions + "\n" : "",
        examples).trim();
  }

  /**
   * Convenience wrapper for when no preferred name is available.
   */
  public String generateUniqueName(String source, java.util.Collection<String> existingNames) {
    return generateUniqueName(source, existingNames, null);
  }

  /**
   * Core generation logic: try LLM up to {@link #MAX_RETRIES} times, sanitize responses, fall back
   * to sanitized source with a counter suffix if all attempts fail.
   *
   * @param source raw text to describe the entity (used for LLM or fallback)
   * @param existingNames names already taken (for uniqueness checks)
   * @param preferred a name to use if it's available and valid
   * @return a unique name
   */
  public String generateUniqueName(String source, java.util.Collection<String> existingNames,
      String preferred) {
    Set<String> taken =
        (existingNames instanceof Set<String> s) ? s : Set.copyOf(existingNames);
    String actualSource = (source == null || source.isBlank()) ? defaultName() : source;

    if (preferred != null && !preferred.isBlank()) {
      String sanitizedPreferred = sanitize(preferred);
      if (!taken.contains(sanitizedPreferred)) {
        return sanitizedPreferred;
      }
    }

    String userPrompt = "Source: " + actualSource;
    if (!taken.isEmpty()) {
      userPrompt += "\n\nThe following names are already taken, choose a different one: "
          + String.join(", ", taken);
    }

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
          if (!taken.contains(name)) {
            return name;
          }
          log.info("LLM generated an existing name: {}. Attempt {}/{}", name, i + 1, MAX_RETRIES);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to generate name via LLM: {}", e.getMessage());
    }

    // Fallback: sanitize the raw source and append a counter if needed
    String name = sanitize(actualSource);
    return appendCounterIfExists(name, taken);
  }

  protected String sanitize(String input) {
    String sanitized = input.toLowerCase()
        .replaceAll("[^a-z0-9_]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");

    if (sanitized.length() > maxLength) {
      sanitized = sanitized.substring(0, maxLength);
      // Clean up potential trailing underscores after truncation
      sanitized = sanitized.replaceAll("_+$", "");
    }
    return sanitized;
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
