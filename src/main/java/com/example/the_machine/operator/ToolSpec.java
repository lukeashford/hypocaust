package com.example.the_machine.operator;

import com.example.the_machine.operator.result.ValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;

/**
 * Specification for a tool/operator including inputs, outputs, validation rules, and metadata.
 * Supports XOR groups for mutually exclusive parameters and JSON schema generation.
 */
@Getter
@Builder
public class ToolSpec {

  private final String name;
  private final String version;
  private final String description;
  @Builder.Default
  private final List<ParamSpec<?>> inputs = new ArrayList<>();
  @Builder.Default
  private final List<ParamSpec<?>> outputs = new ArrayList<>();
  @Builder.Default
  private final List<List<String>> xorGroups = new ArrayList<>();
  @Builder.Default
  private final Map<String, Object> metadata = new HashMap<>();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * Validates input parameters against the specification.
   *
   * @param inputParams the input parameters to validate
   * @return validation result with detailed error messages
   */
  public ValidationResult validate(Map<String, Object> inputParams) {
    final var errors = new ArrayList<String>();

    // Validate each parameter against its specification
    for (final var paramSpec : inputs) {
      final var paramName = paramSpec.getName();
      final var value = inputParams.get(paramName);

      final var result = paramSpec.validate(value);
      if (!result.isOk()) {
        errors.add(result.getMessage());
      }
    }

    // Validate XOR groups
    for (final var xorGroup : xorGroups) {
      final var result = validateXorGroup(xorGroup, inputParams);
      if (!result.isOk()) {
        errors.add(result.getMessage());
      }
    }

    // Check for unexpected parameters
    final var expectedParams = inputs.stream()
        .map(ParamSpec::getName)
        .collect(Collectors.toSet());

    for (final var paramName : inputParams.keySet()) {
      if (!expectedParams.contains(paramName)) {
        errors.add("Unexpected parameter: " + paramName);
      }
    }

    if (errors.isEmpty()) {
      return ValidationResult.success("Tool specification validation completed successfully");
    } else {
      return ValidationResult.error(String.join("; ", errors));
    }
  }

  /**
   * Validates that exactly one parameter in the XOR group is present.
   */
  private ValidationResult validateXorGroup(List<String> xorGroup,
      Map<String, Object> inputParams) {
    if (xorGroup == null || xorGroup.isEmpty()) {
      return ValidationResult.success("XOR group validation completed successfully");
    }

    final var presentParams = xorGroup.stream()
        .filter(
            paramName -> inputParams.containsKey(paramName) && inputParams.get(paramName) != null
        ).toList();

    if (presentParams.isEmpty()) {
      return ValidationResult.xorGroupValidationError(xorGroup,
          "XOR group [" + String.join(", ", xorGroup) +
              "] requires exactly one parameter, but none were provided");
    }

    if (presentParams.size() > 1) {
      return ValidationResult.xorGroupValidationError(xorGroup,
          "XOR group [" + String.join(", ", xorGroup) +
              "] requires exactly one parameter, but multiple were provided: " + presentParams);
    }

    return ValidationResult.success("XOR group validation completed successfully");
  }

  /**
   * Applies default values to input parameters where not already specified.
   *
   * @param inputParams the input parameters (modified in place)
   * @return the modified input parameters map
   */
  public Map<String, Object> applyDefaults(Map<String, Object> inputParams) {
    final var result = new HashMap<>(inputParams);

    for (final var paramSpec : inputs) {
      final var paramName = paramSpec.getName();

      // Apply default if parameter is missing and has a default value
      if (!result.containsKey(paramName) && paramSpec.getDefaultValue() != null) {
        result.put(paramName, paramSpec.getDefaultValue());
      }
    }

    return result;
  }

  /**
   * Creates a redacted copy of input parameters, hiding secret values.
   *
   * @param inputParams the input parameters to redact
   * @return a redacted copy of the parameters
   */
  public Map<String, Object> redactor(Map<String, Object> inputParams) {
    final var result = new HashMap<>(inputParams);

    for (final var paramSpec : inputs) {
      final var paramName = paramSpec.getName();

      if (paramSpec.isSecret() && result.containsKey(paramName)) {
        final var value = result.get(paramName);
        if (value != null) {
          result.put(paramName, redactValue(value.toString()));
        }
      }
    }

    return result;
  }

  private String redactValue(String value) {
    if (value == null || value.length() <= 4) {
      return "***";
    }
    return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
  }

  /**
   * Generates JSON Schema representation of this tool specification.
   *
   * @return JSON Schema as JsonNode
   */
  public JsonNode toJsonSchema() {
    final var schema = OBJECT_MAPPER.createObjectNode();

    // Basic schema metadata
    schema.put("$schema", "http://json-schema.org/draft-07/schema#");
    schema.put("type", "object");
    schema.put("title", name);
    if (description != null) {
      schema.put("description", description);
    }

    // Add tool metadata
    final var toolInfo = schema.putObject("tool");
    toolInfo.put("name", name);
    toolInfo.put("version", version);
    if (description != null) {
      toolInfo.put("description", description);
    }

    // Add custom metadata
    if (!metadata.isEmpty()) {
      final var metadataNode = toolInfo.putObject("metadata");
      metadata.forEach((key, value) -> {
        if (value != null) {
          metadataNode.set(key, OBJECT_MAPPER.valueToTree(value));
        }
      });
    }

    // Input parameters schema
    if (!inputs.isEmpty()) {
      final var inputsSchema = schema.putObject("inputs");
      inputsSchema.put("type", "object");

      final var properties = inputsSchema.putObject("properties");
      final var required = inputsSchema.putArray("required");

      for (final var paramSpec : inputs) {
        final var paramSchema = createParameterSchema(paramSpec);
        properties.set(paramSpec.getName(), paramSchema);

        if (paramSpec.isRequired()) {
          required.add(paramSpec.getName());
        }
      }

      // Add XOR groups as anyOf constraints
      if (!xorGroups.isEmpty()) {
        final var anyOf = inputsSchema.putArray("anyOf");
        for (final var xorGroup : xorGroups) {
          final var xorSchema = OBJECT_MAPPER.createObjectNode();
          final var oneOf = xorSchema.putArray("oneOf");

          for (final var paramName : xorGroup) {
            final var paramRequired = OBJECT_MAPPER.createObjectNode();
            final var requiredArray = paramRequired.putArray("required");
            requiredArray.add(paramName);
            oneOf.add(paramRequired);
          }

          anyOf.add(xorSchema);
        }
      }
    }

    // Output parameters schema
    if (!outputs.isEmpty()) {
      final var outputsSchema = schema.putObject("outputs");
      outputsSchema.put("type", "object");

      final var properties = outputsSchema.putObject("properties");
      final var required = outputsSchema.putArray("required");

      for (final var paramSpec : outputs) {
        final var paramSchema = createParameterSchema(paramSpec);
        properties.set(paramSpec.getName(), paramSchema);

        if (paramSpec.isRequired()) {
          required.add(paramSpec.getName());
        }
      }
    }

    return schema;
  }

  /**
   * Returns the set of input parameter names for capability matching.
   */
  public Set<String> getInputKeys() {
    return inputs.stream()
        .map(ParamSpec::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Returns the set of output parameter names for capability matching.
   */
  public Set<String> getOutputKeys() {
    return outputs.stream()
        .map(ParamSpec::getName)
        .collect(Collectors.toSet());
  }

  /**
   * Creates a JSON Schema for a single parameter.
   */
  private ObjectNode createParameterSchema(ParamSpec<?> paramSpec) {
    final var paramSchema = OBJECT_MAPPER.createObjectNode();
    final var rawClass = paramSpec.getType().getRawClass();

    // Determine JSON Schema type
    if (String.class.equals(rawClass)) {
      paramSchema.put("type", "string");
    } else if (Integer.class.equals(rawClass) || Long.class.equals(rawClass)) {
      paramSchema.put("type", "integer");
    } else if (Double.class.equals(rawClass) || Float.class.equals(rawClass)) {
      paramSchema.put("type", "number");
    } else if (Boolean.class.equals(rawClass)) {
      paramSchema.put("type", "boolean");
    } else if (List.class.isAssignableFrom(rawClass)) {
      paramSchema.put("type", "array");
    } else if (Map.class.isAssignableFrom(rawClass)) {
      paramSchema.put("type", "object");
    } else {
      paramSchema.put("type", "object");
    }

    // Add constraints
    if (paramSpec.getMin() != null) {
      paramSchema.put("minimum", paramSpec.getMin().doubleValue());
    }
    if (paramSpec.getMax() != null) {
      paramSchema.put("maximum", paramSpec.getMax().doubleValue());
    }
    if (paramSpec.getRegex() != null) {
      paramSchema.put("pattern", paramSpec.getRegex());
    }
    if (paramSpec.getEnumValues() != null && !paramSpec.getEnumValues().isEmpty()) {
      final var enumArray = paramSchema.putArray("enum");
      for (final var enumValue : paramSpec.getEnumValues()) {
        enumArray.add(OBJECT_MAPPER.valueToTree(enumValue));
      }
    }
    if (paramSpec.getDefaultValue() != null) {
      paramSchema.set("default", OBJECT_MAPPER.valueToTree(paramSpec.getDefaultValue()));
    }
    if (paramSpec.getDoc() != null) {
      paramSchema.put("description", paramSpec.getDoc());
    }

    // Add custom properties
    if (paramSpec.isSecret()) {
      paramSchema.put("secret", true);
    }
    if (paramSpec.isAdjustable()) {
      paramSchema.put("adjustable", true);
    }

    return paramSchema;
  }

}