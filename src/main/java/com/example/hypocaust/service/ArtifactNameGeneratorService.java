package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for generating unique artifact fileNames from descriptions using an LLM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactNameGeneratorService {

  private static final AnthropicChatModelSpec NAME_GENERATION_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ModelRegistry modelRegistry;

  /**
   * Generate a unique artifact name from description using a small LLM.
   */
  public String generateUniqueName(String description, Set<String> existingNames) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(NAME_GENERATION_MODEL))
          .build();

      for (int i = 0; i < 3; i++) {
        String response = chatClient.prompt()
            .system("""
                Generate a short snake_case name for an artifact (max 30 chars).
                Use only lowercase letters, numbers, and underscores.
                Reply with ONLY the name, nothing else.
                Examples: hero_portrait, forest_background, main_script
                """)
            .user(buildUserPrompt(description, existingNames))
            .call()
            .content();

        if (response != null && !response.isBlank()) {
          String name = sanitize(response);
          if (!existingNames.contains(name)) {
            return name;
          }
          log.info("LLM generated an existing name: {}. Attempt {}/3", name, i + 1);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to generate artifact name via LLM: {}", e.getMessage());
    }

    // Fallback: generate from description
    String name = sanitize(description);
    if (name.length() > 30) {
      name = name.substring(0, 30);
    }

    return appendCounterIfExists(name, existingNames);
  }

  private String buildUserPrompt(String description, Set<String> existingNames) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("The artifact's description: ").append(description);
    if (!existingNames.isEmpty()) {
      prompt.append("\n\nThe following names are already taken, choose a different one: ");
      prompt.append(String.join(", ", existingNames));
    }
    return prompt.toString();
  }

  private String sanitize(String input) {
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
