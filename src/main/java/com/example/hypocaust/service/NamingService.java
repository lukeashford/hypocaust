package com.example.hypocaust.service;

import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating unique technical identifiers (snake_case) using an LLM. Handles artifact
 * names and task execution names with collision checks and retries via the unified ChatService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NamingService {

  private static final AnthropicChatModelSpec MODEL = AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private final ChatService chatService;

  /**
   * Derives a unique snake_case name from a human-readable title.
   */
  public String deriveNameFromTitle(String title, Collection<String> existing) {
    if (title == null || title.isBlank()) {
      return generateArtifactName("unnamed_artifact", existing);
    }
    String baseName = sanitize(title, 30);
    Set<String> taken = (existing instanceof Set<String> s) ? s : Set.copyOf(existing);
    return appendCounterIfExists(baseName, taken);
  }

  /**
   * Generates a unique artifact name.
   */
  public String generateArtifactName(String source, Collection<String> existing) {
    return generateUnique(PromptFragments.artifactName(), source, existing, 30, "artifact");
  }

  /**
   * Generates a unique artifact name with a preferred choice.
   */
  public String generateArtifactName(String source, Collection<String> existing, String preferred) {
    return generateUnique(PromptFragments.artifactName(), source, existing, 30, "artifact",
        preferred);
  }

  /**
   * Generates a unique task execution name.
   */
  public String generateExecutionName(String source, Collection<String> existing) {
    return generateUnique(PromptFragments.taskExecutionName(), source, existing, 50, "task");
  }

  private String generateUnique(PromptFragment fragment, String source, Collection<String> existing,
      int maxLen, String defaultName) {
    return generateUnique(fragment, source, existing, maxLen, defaultName, null);
  }

  private String generateUnique(PromptFragment fragment, String source, Collection<String> existing,
      int maxLen, String defaultName, String preferred) {

    Set<String> taken = (existing instanceof Set<String> s) ? s : Set.copyOf(existing);
    String actualSource = (source == null || source.isBlank()) ? defaultName : source;

    if (preferred != null && !preferred.isBlank()) {
      String sanitizedPreferred = sanitize(preferred, maxLen);
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
      String response = chatService.call(MODEL, fragment.text(), userPrompt);

      if (response != null && !response.isBlank()) {
        String name = sanitize(response, maxLen);
        if (!taken.contains(name)) {
          return name;
        }
        log.info("LLM generated an existing name: {}", name);
      }
    } catch (Exception e) {
      log.warn("Failed to generate unique name via LLM: {}", e.getMessage());
    }

    // Fallback: sanitize the raw source and append a counter if needed
    String name = sanitize(actualSource, maxLen);
    return appendCounterIfExists(name, taken);
  }

  private String sanitize(String input, int maxLen) {
    String sanitized = input.toLowerCase()
        .replaceAll("[^a-z0-9_]", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");

    if (sanitized.length() > maxLen) {
      sanitized = sanitized.substring(0, maxLen);
      sanitized = sanitized.replaceAll("_+$", "");
    }
    return sanitized;
  }

  private String appendCounterIfExists(String name, Set<String> taken) {
    String result = name;
    int counter = 2;
    while (taken.contains(result)) {
      result = name + "_" + counter;
      counter++;
    }
    return result;
  }
}
