package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.Result;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Simplified parameter specification containing only essential attributes.
 * Supports optional default values for non-required parameters.
 */
public record ParamSpec<T>(
    String name,
    JavaType type,
    String description,
    boolean required,
    T defaultValue
) {

  /**
   * Returns true if this parameter has a default value.
   */
  public boolean hasDefault() {
    return defaultValue != null;
  }

  /**
   * Validates a value against this parameter specification.
   *
   * @param value the value to validate
   * @return validation result with error message if invalid
   */
  public Result validate(Object value) {
    // Check required constraint
    if (required && value == null) {
      return Result.failure("Parameter '" + name + "' is required but was null");
    }

    if (value == null) {
      return Result.success("Parameter '" + name + "' validation completed successfully");
    }

    // Basic type validation based on string type
    @SuppressWarnings("unchecked")
    T castValue = type.getRawClass().isInstance(value) ? (T) value : null;
    if (castValue == null) {
      return Result.failure(
          "Parameter '" + name + "' expected type " + type.getRawClass().getSimpleName()
              + " but got " + value.getClass().getSimpleName());
    }

    return Result.success("Parameter '" + name + "' validation completed successfully");
  }

  // Factory methods for required parameters (no default)

  public static ParamSpec<String> string(String name, String description, boolean required) {
    return new ParamSpec<>(
        name,
        TypeFactory.defaultInstance().constructType(String.class),
        description,
        required,
        null
    );
  }

  public static ParamSpec<Integer> integer(String name, String description, boolean required) {
    return new ParamSpec<>(
        name,
        TypeFactory.defaultInstance().constructType(Integer.class),
        description,
        required,
        null
    );
  }

  // Factory methods for optional parameters with defaults

  public static ParamSpec<String> string(String name, String description, String defaultValue) {
    return new ParamSpec<>(
        name,
        TypeFactory.defaultInstance().constructType(String.class),
        description,
        false,
        defaultValue
    );
  }

  public static ParamSpec<Integer> integer(String name, String description, int defaultValue) {
    return new ParamSpec<>(
        name,
        TypeFactory.defaultInstance().constructType(Integer.class),
        description,
        false,
        defaultValue
    );
  }
}