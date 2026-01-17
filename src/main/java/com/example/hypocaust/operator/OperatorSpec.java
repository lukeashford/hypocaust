package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simplified operator specification containing only essential attributes.
 */
public record OperatorSpec(
    String name, String version, String description,
    List<ParamSpec<?>> inputs,
    List<ParamSpec<?>> outputs
) {

  /**
   * Normalizes input parameters by applying default values for missing or blank optional parameters.
   * Returns a new map with defaults applied - does not modify the original.
   *
   * @param inputParams the raw input parameters
   * @return normalized parameters with defaults applied
   */
  public Map<String, Object> normalize(Map<String, Object> inputParams) {
    final var normalized = new HashMap<>(inputParams);

    for (final var paramSpec : inputs) {
      if (!paramSpec.hasDefault()) {
        continue;
      }

      final var paramName = paramSpec.name();
      final var value = normalized.get(paramName);

      // Apply default if value is null or (for strings) blank
      if (shouldApplyDefault(value)) {
        normalized.put(paramName, paramSpec.defaultValue());
      }
    }

    return normalized;
  }

  private boolean shouldApplyDefault(Object value) {
    if (value == null) {
      return true;
    }
    // For strings, also apply default if blank
    if (value instanceof String str) {
      return str.isBlank();
    }
    return false;
  }

  /**
   * Validates input parameters against the specification.
   *
   * @param inputParams the input parameters to validate
   * @return validation result with detailed error messages
   */
  public Result validate(Map<String, Object> inputParams) {
    final var errors = new ArrayList<String>();

    // Validate each parameter against its specification
    for (final var paramSpec : inputs) {
      final var paramName = paramSpec.name();
      final var value = inputParams.get(paramName);

      final var result = paramSpec.validate(value);
      if (!result.ok()) {
        errors.add(result.message());
      }
    }

    final var expectedKeys = getInputKeys();
    for (final var paramName : inputParams.keySet()) {
      if (!expectedKeys.contains(paramName)) {
        errors.add("Unexpected parameter: " + paramName);
      }
    }

    if (errors.isEmpty()) {
      return Result.success("Operator specification validation completed successfully");
    } else {
      return Result.failure(String.join("; ", errors));
    }
  }

  /**
   * Returns the set of input parameter names.
   */
  public List<String> getInputKeys() {
    return inputs.stream()
        .map(ParamSpec::name)
        .toList();
  }

  public List<String> getOutputKeys() {
    return outputs.stream()
        .map(ParamSpec::name)
        .toList();
  }
}