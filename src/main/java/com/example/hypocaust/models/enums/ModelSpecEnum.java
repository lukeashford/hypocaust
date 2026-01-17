package com.example.hypocaust.models.enums;

import com.example.hypocaust.exception.ModelException;
import java.util.Arrays;

/**
 * Common interface for model specification enums that need JsonCreator functionality. Provides a
 * generic fromString method to eliminate code duplication across model enums.
 */
public interface ModelSpecEnum {

  String getModelName();

  /**
   * Generic fromString method for model enums that eliminates boilerplate code.
   *
   * @param modelName the model name string from configuration
   * @param enumClass the enum class to search within
   * @param modelType descriptive name for error messages
   * @param <T> enum type that extends both Enum and ModelSpecEnum
   * @return the matching enum constant
   * @throws ModelException if the model name is not recognized
   */
  static <T extends Enum<T> & ModelSpecEnum> T fromString(
      String modelName,
      Class<T> enumClass,
      String modelType) throws ModelException {
    return Arrays.stream(enumClass.getEnumConstants())
        .filter(model -> model.getModelName().equals(modelName))
        .findFirst()
        .orElseThrow(() -> new ModelException("Unknown " + modelType + " model: " + modelName));
  }
}