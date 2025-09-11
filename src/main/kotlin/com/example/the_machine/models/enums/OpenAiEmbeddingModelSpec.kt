package com.example.the_machine.models.enums

import com.example.the_machine.exception.ModelException
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Enum representing OpenAI embedding models available in the application configuration.
 */
enum class OpenAiEmbeddingModelSpec(override val modelName: String) : ModelSpecEnum {

  TEXT_EMBEDDING_3_SMALL("text-embedding-3-small");

  companion object {

    /**
     * Creates an enum instance from a string model name for fail-fast validation. Used by Spring Boot
     * for YAML string to enum conversion.
     *
     * @param modelName the model name string from configuration
     * @return the corresponding enum constant
     * @throws ModelException if the model name is not recognized
     */
    @JsonCreator
    @JvmStatic
    fun fromString(modelName: String): OpenAiEmbeddingModelSpec {
      return ModelSpecEnum.fromString(modelName, "OpenAI embedding")
    }
  }
}