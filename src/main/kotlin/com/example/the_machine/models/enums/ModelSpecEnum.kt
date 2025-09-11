package com.example.the_machine.models.enums

import com.example.the_machine.exception.ModelException

/**
 * Common interface for model specification enums that need JsonCreator functionality. Provides a
 * generic fromString method to eliminate code duplication across model enums.
 */
interface ModelSpecEnum {

  val modelName: String

  companion object {

    /**
     * Generic fromString method for model enums that eliminates boilerplate code.
     * Uses Kotlin's reified type parameters and inline functions for improved performance.
     *
     * @param modelName the model name string from configuration
     * @param modelType descriptive name for error messages
     * @param T enum type that implements ModelSpecEnum
     * @return the matching enum constant
     * @throws ModelException if the model name is not recognized
     */
    inline fun <reified T> fromString(
      modelName: String,
      modelType: String
    ): T where T : Enum<T>, T : ModelSpecEnum {
      return enumValues<T>()
        .find { it.modelName == modelName }
        ?: throw ModelException("Unknown $modelType model: $modelName")
    }
  }
}