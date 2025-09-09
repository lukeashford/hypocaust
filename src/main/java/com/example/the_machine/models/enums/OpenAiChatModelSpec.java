package com.example.the_machine.models.enums;

import com.example.the_machine.exception.ModelException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing OpenAI chat models available in the application configuration.
 */
@Getter
@RequiredArgsConstructor
public enum OpenAiChatModelSpec implements ModelSpecEnum {
  GPT_5("gpt-5"),
  GPT_4O("gpt-4o"),
  GPT_4O_MINI("gpt-4o-mini"),
  O3("o3");

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
  public static OpenAiChatModelSpec fromString(String modelName) throws ModelException {
    return ModelSpecEnum.fromString(modelName, OpenAiChatModelSpec.class, "OpenAI chat");
  }
}