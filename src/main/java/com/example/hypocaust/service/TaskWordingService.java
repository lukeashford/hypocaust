package com.example.hypocaust.service;

import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Service for generating human-friendly labels and summaries for tasks and execution results.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskWordingService {

  private static final AnthropicChatModelSpec WORDING_MODEL =
      AnthropicChatModelSpec.CLAUDE_3_5_HAIKU_LATEST;

  private final ModelRegistry modelRegistry;

  /**
   * Generates a brief progress label (1-5 words) for a task.
   */
  public String generateTodoWording(String task) {
    return generate(task, """
        Generate a brief progress label (1-5 words) for this task.
        Focus on the action. Start with a present participle like 'Adding', 'Creating', 'Updating'.
        Output ONLY the label.
        """);
  }

  /**
   * Generates a brief commit message (1 sentence) for a completed task.
   */
  public String generateCommitMessage(String task) {
    return generate(task, """
        Generate a brief commit message (1 sentence, max 100 chars) summarizing what was done.
        Focus on the outcome, not the process. Start with a verb like 'Added', 'Created', 'Updated'.
        Output ONLY the message.
        """);
  }

  private String generate(String task, String systemPrompt) {
    try {
      ChatClient chatClient = ChatClient.builder(modelRegistry.get(WORDING_MODEL)).build();

      String response = chatClient.prompt()
          .system(systemPrompt)
          .user("Task: " + task)
          .call()
          .content();

      if (response != null && !response.isBlank()) {
        return response.trim().replaceAll("^\"|\"$", "");
      }
    } catch (Exception e) {
      log.warn("Failed to generate wording, using fallback: {}", e.getMessage());
    }
    return "Processing task";
  }
}
