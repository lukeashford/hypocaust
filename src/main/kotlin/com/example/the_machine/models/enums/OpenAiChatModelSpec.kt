package com.example.the_machine.models.enums

import com.example.the_machine.exception.ModelException
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Enum representing OpenAI chat models available in the application configuration.
 */
enum class OpenAiChatModelSpec(override val modelName: String) : ModelSpecEnum {

  GPT_5("gpt-5"),
  GPT_4O("gpt-4o"),
  GPT_4O_MINI("gpt-4o-mini"),
  O3("o3");

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
    fun fromString(modelName: String): OpenAiChatModelSpec {
      return ModelSpecEnum.fromString(modelName, "OpenAI chat")
    }
  }
}