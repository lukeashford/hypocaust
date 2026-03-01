package com.example.hypocaust.service;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.exception.ExternalServiceException;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.PromptFragment;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.rag.ModelRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for generating human-friendly non-unique wording (titles, descriptions, messages) using
 * the unified ChatService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WordingService {

  private static final AnthropicChatModelSpec WORDING_MODEL =
      AnthropicChatModelSpec.CLAUDE_HAIKU_4_5;

  private final ChatService chatService;
  private final ObjectMapper objectMapper;

  /**
   * Generates a brief progress label for a task.
   */
  public String generateTodoWording(String task) {
    return truncateWithEllipsis(
        generate(PromptFragments.todoLabel(), task, "Task to convert to todo: "), 80);
  }

  /**
   * Generates a brief commit message (1 sentence) for a completed task.
   */
  public String generateCommitMessage(String task) {
    return truncateWithEllipsis(generate(PromptFragments.commitMessage(), task,
        "Task to make commit message from: "), 100);
  }

  /**
   * Translates a task into structured model requirements.
   */
  public ModelRequirement generateModelRequirement(String task) {
    String response = generate(PromptFragments.modelRequirement(),
        String.format("Task: %s", task),
        "Requirement analysis: ");

    try {
      return objectMapper.readValue(JsonUtils.extractJson(response), ModelRequirement.class);
    } catch (Exception e) {
      log.warn("Failed to parse model requirements, using defaults", e);
      throw new ExternalServiceException("Generated model requirements could not be parsed.", e);
    }
  }

  private String generate(PromptFragment fragment, String source, String userPrefix) {
    try {
      String response = chatService.call(WORDING_MODEL, fragment.text(), userPrefix + source);

      if (response != null && !response.isBlank()) {
        return response.trim().replaceAll("^\"|\"$", "");
      }
    } catch (Exception e) {
      log.warn("Failed to generate wording for {}: {}", fragment.id(), e.getMessage());
    }
    return source;
  }

  private String truncateWithEllipsis(String text, int maxLength) {
    if (text.length() <= maxLength) {
      return text;
    }
    return text.substring(0, maxLength - 3) + "...";
  }
}
