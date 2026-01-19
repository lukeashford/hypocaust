package com.example.hypocaust.models.enums;

import com.example.hypocaust.exception.ModelException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing Anthropic chat models available in the application configuration.
 */
@Getter
@RequiredArgsConstructor
public enum AnthropicChatModelSpec implements ModelSpecEnum {
  CLAUDE_OPUS_4_5_LATEST("claude-opus-4-5-latest"),
  CLAUDE_SONNET_4_LATEST("claude-sonnet-4-latest"),
  CLAUDE_HAIKU_3_5_LATEST("claude-3-5-haiku-latest");

  private final String modelName;

  /**
   * Creates an enum instance from a string model name for fail-fast validation. Used by Spring Boot
   * for YAML string to enum conversion.
   *
   * @param modelName the model name string from configuration
   * @return the corresponding enum constant
   * @throws IllegalArgumentException if the model name is not recognized
   */
  @JsonCreator
  public static AnthropicChatModelSpec fromString(String modelName) throws ModelException {
    return ModelSpecEnum.fromString(modelName, AnthropicChatModelSpec.class, "Anthropic chat");
  }
}