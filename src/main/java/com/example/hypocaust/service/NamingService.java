package com.example.hypocaust.service;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public record ArtifactNaming(String title, String name, String description) {

  }

  /**
   * Consolidates generation of title, name, and description into one LLM call.
   */
  public ArtifactNaming generateArtifactNaming(String task, String outputDescription,
      ArtifactKind kind, Set<String> takenNames, Set<String> takenTitles) {

    String userPrompt = String.format("Task: %s\nKind: %s\nRole: %s", task, kind,
        outputDescription);

    if (!takenNames.isEmpty()) {
      userPrompt += "\nTaken names: " + String.join(", ", takenNames);
    }
    if (!takenTitles.isEmpty()) {
      userPrompt += "\nTaken titles: " + String.join(", ", takenTitles);
    }

    // Default fallbacks in case LLM fails
    String title = task;
    String description = "Generated " + kind.name().toLowerCase();
    String name = sanitize(task, 30);

    try {
      String response = chatService.call(MODEL, PromptFragments.artifactNaming().text(),
          userPrompt);
      if (response != null && !response.isBlank()) {
        JsonNode json = objectMapper.readTree(JsonUtils.extractJson(response));
        title = json.path("title").asText(title);
        name = json.path("name").asText(name);
        description = json.path("description").asText(description);
      }
    } catch (Exception e) {
      log.warn("Failed to generate naming via LLM: {}", e.getMessage());
    }

    // Enforce uniqueness
    title = appendCounterIfExists(title, takenTitles);
    name = sanitize(name, 30);
    name = appendCounterIfExists(name, takenNames);

    return new ArtifactNaming(title, name, description);
  }

  /**
   * Generates a unique task execution name.
   */
  public String generateExecutionName(String source, Set<String> taken) {
    String actualSource = (source == null || source.isBlank()) ? "task" : source;

    String userPrompt = "Source: " + actualSource;
    if (!taken.isEmpty()) {
      userPrompt += "\n\nThe following names are already taken, choose a different one: "
          + String.join(", ", taken);
    }

    try {
      String response = chatService.call(MODEL, PromptFragments.taskExecutionName().text(),
          userPrompt);

      if (response != null && !response.isBlank()) {
        String name = sanitize(response, 50);
        if (!taken.contains(name)) {
          return name;
        }
        log.info("LLM generated an existing name: {}", name);
      }
    } catch (Exception e) {
      log.warn("Failed to generate unique execution name via LLM: {}", e.getMessage());
    }

    // Fallback: sanitize the raw source and append a counter if needed
    String name = sanitize(actualSource, 50);
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
