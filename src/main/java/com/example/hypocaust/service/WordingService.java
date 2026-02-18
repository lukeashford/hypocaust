package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.WordingFragments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for generating human-friendly non-unique wording (titles, descriptions, messages).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordingService {

  private static final AnthropicChatModelSpec WORDING_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ModelRegistry modelRegistry;

  /**
   * Generates a brief progress label (1-5 words) for a task.
   */
  public String generateTodoWording(String task) {
    return generate(WordingFragments.todoLabel(), task, "Task: ");
  }

  /**
   * Generates a brief commit message (1 sentence) for a completed task.
   */
  public String generateCommitMessage(String task) {
    return generate(WordingFragments.commitMessage(), task, "Task: ");
  }

  /**
   * Generates a catchy title for an artifact.
   */
  public String generateArtifactTitle(String source) {
    return generate(WordingFragments.artifactTitle(), source,
        "Generation Prompt to name/describe: ");
  }

  /**
   * Generates a brief description for an artifact.
   */
  public String generateArtifactDescription(String source) {
    return generate(WordingFragments.artifactDescription(), source,
        "Generation Prompt to name/describe: ");
  }

  private String generate(PromptFragment fragment, String source, String userPrefix) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(WORDING_MODEL)).build();

      String response = chatClient.prompt()
          .system(fragment.text())
          .user(userPrefix + source)
          .call()
          .content();

      if (response != null && !response.isBlank()) {
        return response.trim().replaceAll("^\"|\"$", "");
      }
    } catch (Exception e) {
      log.warn("Failed to generate wording for {}: {}", fragment.id(), e.getMessage());
    }
    return "Processing";
  }
}
