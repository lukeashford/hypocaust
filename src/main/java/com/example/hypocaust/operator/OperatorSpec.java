package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.Result;
import java.util.ArrayList;
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