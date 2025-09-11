package com.example.the_machine.models.enums

import com.example.the_machine.exception.ModelException
import com.fasterxml.jackson.annotation.JsonCreator

/**
 * Enum representing Anthropic chat models available in the application configuration.
 */
enum class AnthropicChatModelSpec(override val modelName: String) : ModelSpecEnum {

  CLAUDE_SONNET_4_LATEST("claude-sonnet-4-latest");

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
    fun fromString(modelName: String): AnthropicChatModelSpec {
      return ModelSpecEnum.fromString(modelName, "Anthropic chat")
    }
  }
}