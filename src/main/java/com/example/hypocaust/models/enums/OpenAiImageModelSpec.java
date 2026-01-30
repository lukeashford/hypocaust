package com.example.hypocaust.models.enums;

import com.example.hypocaust.exception.ModelException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing OpenAI image models available in the application.
 */
@Getter
@RequiredArgsConstructor
public enum OpenAiImageModelSpec implements ModelSpecEnum {
  DALL_E_3("dall-e-3"),
  DALL_E_2("dall-e-2");

  private final String modelName;

  /**
   * Creates an enum instance from a string model fileName for fail-fast validation.
   *
   * @param modelName the model fileName string from configuration
   * @return the corresponding enum constant
   * @throws ModelException if the model fileName is not recognized
   */
  @JsonCreator
  public static OpenAiImageModelSpec fromString(String modelName) throws ModelException {
    return ModelSpecEnum.fromString(modelName, OpenAiImageModelSpec.class, "OpenAI image");
  }
}
