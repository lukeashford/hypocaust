package com.example.hypocaust.service;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.ArtifactKind;
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
   * Generates a brief progress label (1-5 words) for a task.
   */
  public String generateTodoWording(String task) {
    String label = generate(PromptFragments.todoLabel(), task, "Task to convert to todo: ");
    return label.length() > 50 ? label.substring(0, 47) + "..." : label;
  }

  /**
   * Generates a brief commit message (1 sentence) for a completed task.
   */
  public String generateCommitMessage(String task) {
    return generate(PromptFragments.commitMessage(), task, "Task to make commit message from: ");
  }

  /**
   * Generates a catchy title for an artifact.
   */
  public String generateArtifactTitle(String source) {
    return generate(PromptFragments.artifactTitle(), source,
        "Generation Prompt to name: ");
  }

  /**
   * Generates a brief description for an artifact.
   */
  public String generateArtifactDescription(String source) {
    String desc = generate(PromptFragments.artifactDescription(), source,
        "Generation Prompt to describe: ");
    return desc.length() > 100 ? desc.substring(0, 97) + "..." : desc;
  }

  /**
   * Translates a task into structured model requirements.
   */
  public ModelRequirement generateModelRequirement(String task, ArtifactKind targetKind) {
    String response = generate(PromptFragments.modelRequirement(),
        String.format("Task: %s, Target: %s", task, targetKind),
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
}
