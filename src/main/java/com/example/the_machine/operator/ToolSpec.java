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
import lombok.val;

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
    val errors = new ArrayList<String>();

    // Validate each parameter against its specification
    for (val paramSpec : inputs) {
      val paramName = paramSpec.getName();
      val value = inputParams.get(paramName);

      val result = paramSpec.validate(value);
      if (!result.isOk()) {
        errors.add(result.getMessage());
      }
    }

    // Validate XOR groups
    for (val xorGroup : xorGroups) {
      val result = validateXorGroup(xorGroup, inputParams);
      if (!result.isOk()) {
        errors.add(result.getMessage());
      }
    }

    // Check for unexpected parameters
    val expectedParams = inputs.stream()
        .map(ParamSpec::getName)
        .collect(Collectors.toSet());

    for (val paramName : inputParams.keySet()) {
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

    val presentParams = xorGroup.stream()
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
    val result = new HashMap<>(inputParams);

    for (val paramSpec : inputs) {
      val paramName = paramSpec.getName();

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
    val result = new HashMap<>(inputParams);

    for (val paramSpec : inputs) {
      val paramName = paramSpec.getName();

      if (paramSpec.isSecret() && result.containsKey(paramName)) {
        val value = result.get(paramName);
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
    val schema = OBJECT_MAPPER.createObjectNode();

    // Basic schema metadata
    schema.put("$schema", "http://json-schema.org/draft-07/schema#");
    schema.put("type", "object");
    schema.put("title", name);
    if (description != null) {
      schema.put("description", description);
    }

    // Add tool metadata
    val toolInfo = schema.putObject("tool");
    toolInfo.put("name", name);
    toolInfo.put("version", version);
    if (description != null) {
      toolInfo.put("description", description);
    }

    // Add custom metadata
    if (!metadata.isEmpty()) {
      val metadataNode = toolInfo.putObject("metadata");
      metadata.forEach((key, value) -> {
        if (value != null) {
          metadataNode.set(key, OBJECT_MAPPER.valueToTree(value));
        }
      });
    }

    // Input parameters schema
    if (!inputs.isEmpty()) {
      val inputsSchema = schema.putObject("inputs");
      inputsSchema.put("type", "object");

      val properties = inputsSchema.putObject("properties");
      val required = inputsSchema.putArray("required");

      for (val paramSpec : inputs) {
        val paramSchema = createParameterSchema(paramSpec);
        properties.set(paramSpec.getName(), paramSchema);

        if (paramSpec.isRequired()) {
          required.add(paramSpec.getName());
        }
      }

      // Add XOR groups as anyOf constraints
      if (!xorGroups.isEmpty()) {
        val anyOf = inputsSchema.putArray("anyOf");
        for (val xorGroup : xorGroups) {
          val xorSchema = OBJECT_MAPPER.createObjectNode();
          val oneOf = xorSchema.putArray("oneOf");

          for (val paramName : xorGroup) {
            val paramRequired = OBJECT_MAPPER.createObjectNode();
            val requiredArray = paramRequired.putArray("required");
            requiredArray.add(paramName);
            oneOf.add(paramRequired);
          }

          anyOf.add(xorSchema);
        }
      }
    }

    // Output parameters schema
    if (!outputs.isEmpty()) {
      val outputsSchema = schema.putObject("outputs");
      outputsSchema.put("type", "object");

      val properties = outputsSchema.putObject("properties");
      val required = outputsSchema.putArray("required");

      for (val paramSpec : outputs) {
        val paramSchema = createParameterSchema(paramSpec);
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
    val paramSchema = OBJECT_MAPPER.createObjectNode();
    val rawClass = paramSpec.getType().getRawClass();

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
      val enumArray = paramSchema.putArray("enum");
      for (val enumValue : paramSpec.getEnumValues()) {
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