package com.example.hypocaust.service;

import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.exception.ExternalServiceException;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.rag.ModelRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for translating tasks into structured model requirements using the unified ChatService.
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
   * Translates a task into structured model requirements.
   */
  public ModelRequirement generateModelRequirement(String task, Set<ArtifactKind> outputKinds) {
    String response;
    try {
      response = chatService.call(WORDING_MODEL,
          PromptFragments.modelRequirement().text(),
          "Requirement analysis for the following task: " + task);

      if (response != null && !response.isBlank()) {
        response = response.trim().replaceAll("^\"|\"$", "");
      } else {
        throw new ExternalServiceException("Empty response from model requirement generation.",
            null);
      }
    } catch (ExternalServiceException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Failed to generate model requirement: {}", e.getMessage());
      throw new ExternalServiceException("Model requirement generation failed.", e);
    }

    try {
      ModelRequirement req = objectMapper.readValue(JsonUtils.extractJson(response),
          ModelRequirement.class);
      return req.toBuilder()
          .outputs(outputKinds)
          .build();
    } catch (Exception e) {
      log.warn("Failed to parse model requirements, using defaults", e);
      throw new ExternalServiceException("Generated model requirements could not be parsed.", e);
    }
  }
}
