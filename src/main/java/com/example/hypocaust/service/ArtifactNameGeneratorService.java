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
   * Generate a unique artifact fileName from description using a small LLM.
   */
  public String generateUniqueName(String description, Set<String> existingNames) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(NAME_GENERATION_MODEL))
          .build();

      StringBuilder prompt = new StringBuilder();
      prompt.append("Generate a short, snake_case artifact fileName for: ").append(description);
      if (!existingNames.isEmpty()) {
        prompt.append("\n\nThe following names are already taken, choose a different one: ");
        prompt.append(String.join(", ", existingNames));
      }

      String response = chatClient.prompt()
          .system("""
              Generate a short snake_case fileName for an artifact (max 30 chars).
              Use only lowercase letters, numbers, and underscores.
              Reply with ONLY the fileName, nothing else.
              Examples: hero_portrait, forest_background, main_script
              """)
          .user(prompt.toString())
          .call()
          .content();

      if (response != null && !response.isBlank()) {
        String name = response.trim().toLowerCase()
            .replaceAll("[^a-z0-9_]", "_")
            .replaceAll("_+", "_")
            .replaceAll("^_|_$", "");

        // Truncate if too long
        if (name.length() > 30) {
          name = name.substring(0, 30);
        }

        // If fileName is still taken, append a number
        String baseName = name;
        int counter = 2;
        while (existingNames.contains(name)) {
          name = baseName + "_" + counter;
          counter++;
        }

        return name;
      }
    } catch (Exception e) {
      log.warn("Failed to generate artifact fileName via LLM: {}", e.getMessage());
    }

    // Fallback: generate from description
    String name = description.toLowerCase()
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("^_|_$", "");
    if (name.length() > 30) {
      name = name.substring(0, 30);
    }

    // If fileName is taken, append a number
    String baseName = name;
    int counter = 2;
    while (existingNames.contains(name)) {
      name = baseName + "_" + counter;
      counter++;
    }

    return name;
  }
}
