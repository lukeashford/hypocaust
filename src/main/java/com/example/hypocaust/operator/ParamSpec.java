package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.Result;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Simplified parameter specification containing only essential attributes.
 */
public record ParamSpec<T>(String name, JavaType type, String description, boolean required) {

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

  public static ParamSpec<String> string(String name, String description, boolean required) {
    return new ParamSpec<>(
        name,
        TypeFactory.defaultInstance().constructType(String.class),
        description,
        required
    );
  }

  public static ParamSpec<Integer> integer(String name, String description, boolean required) {
    return new ParamSpec<>(
        name,
        TypeFactory.defaultInstance().constructType(Integer.class),
        description,
        required
    );
  }

}