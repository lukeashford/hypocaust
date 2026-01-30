package com.example.hypocaust.models.enums;

import com.example.hypocaust.exception.ModelException;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing OpenAI embedding models available in the application configuration.
 */
@Getter
@RequiredArgsConstructor
public enum OpenAiEmbeddingModelSpec implements ModelSpecEnum {
  TEXT_EMBEDDING_3_SMALL("text-embedding-3-small");

  private final String modelName;

  /**
   * Creates an enum instance from a string model fileName for fail-fast validation. Used by Spring
   * Boot for YAML string to enum conversion.
   *
   * @param modelName the model fileName string from configuration
   * @return the corresponding enum constant
   * @throws IllegalArgumentException if the model fileName is not recognized
   */
  @JsonCreator
  public static OpenAiEmbeddingModelSpec fromString(String modelName) throws ModelException {
    return ModelSpecEnum.fromString(modelName, OpenAiEmbeddingModelSpec.class, "OpenAI embedding");
  }
}