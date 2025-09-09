package com.example.the_machine.models.enums;

import com.example.the_machine.exception.ModelException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing Anthropic chat models available in the application configuration.
 */
@Getter
@RequiredArgsConstructor
public enum AnthropicChatModelSpec implements ModelSpecEnum {
  CLAUDE_SONNET_4_LATEST("claude-sonnet-4-latest");

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