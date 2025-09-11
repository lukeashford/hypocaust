package com.example.the_machine.operator

import com.example.the_machine.operator.result.ValidationResult
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Specification for a tool/operator including inputs, outputs, validation rules, and metadata.
 * Supports XOR groups for mutually exclusive parameters and JSON schema generation.
 */
data class ToolSpec(
  val name: String,
  val version: String,
  val description: String? = null,
  val inputs: List<ParamSpec<*>> = emptyList(),
  val outputs: List<ParamSpec<*>> = emptyList(),
  val xorGroups: List<List<String>> = emptyList(),
  val metadata: Map<String, Any> = emptyMap()
) {

  companion object {

    private val OBJECT_MAPPER = ObjectMapper()

  }

  /**
   * Validates input parameters against the specification.
   *
   * @param inputParams the input parameters to validate
   * @return validation result with detailed error messages
   */
  fun validate(inputParams: Map<String, Any>): ValidationResult {
    val errors = mutableListOf<String>()

    // Validate each parameter against its specification
    for (paramSpec in inputs) {
      val paramName = paramSpec.name
      val value = inputParams[paramName]

      val result = paramSpec.validate(value)
      if (!result.ok) {
        errors.add(result.message)
      }
    }

    // Validate XOR groups
    for (xorGroup in xorGroups) {
      val result = validateXorGroup(xorGroup, inputParams)
      if (!result.ok) {
        errors.add(result.message)
      }
    }

    // Check for unexpected parameters
    val expectedParams = inputs.map { it.name }.toSet()

    for (paramName in inputParams.keys) {
      if (paramName !in expectedParams) {
        errors.add("Unexpected parameter: $paramName")
      }
    }

    return if (errors.isEmpty()) {
      ValidationResult.success("Tool specification validation completed successfully")
    } else {
      ValidationResult.error(errors.joinToString("; "))
    }
  }

  /**
   * Validates that exactly one parameter in the XOR group is present.
   */
  private fun validateXorGroup(
    xorGroup: List<String>,
    inputParams: Map<String, Any>
  ): ValidationResult {
    if (xorGroup.isEmpty()) {
      return ValidationResult.success("XOR group validation completed successfully")
    }

    val presentParams = xorGroup.filter { paramName ->
      inputParams.containsKey(paramName) && inputParams[paramName] != null
    }

    return when {
      presentParams.isEmpty() -> ValidationResult.xorGroupValidationError(
        xorGroup,
        "XOR group [${xorGroup.joinToString(", ")}] requires exactly one parameter, but none were provided"
      )

      presentParams.size > 1 -> ValidationResult.xorGroupValidationError(
        xorGroup,
        "XOR group [${xorGroup.joinToString(", ")}] requires exactly one parameter, but multiple were provided: $presentParams"
      )

      else -> ValidationResult.success("XOR group validation completed successfully")
    }
  }

  /**
   * Applies default values to input parameters where not already specified.
   *
   * @param inputParams the input parameters (modified in place)
   * @return the modified input parameters map
   */
  fun applyDefaults(inputParams: Map<String, Any>): Map<String, Any> {
    val result = inputParams.toMutableMap()

    for (paramSpec in inputs) {
      val paramName = paramSpec.name

      // Apply default if parameter is missing and has a default value
      if (!result.containsKey(paramName) && paramSpec.defaultValue != null) {
        result[paramName] = paramSpec.defaultValue
      }
    }

    return result
  }

  /**
   * Creates a redacted copy of input parameters, hiding secret values.
   *
   * @param inputParams the input parameters to redact
   * @return a redacted copy of the parameters
   */
  fun redactor(inputParams: Map<String, Any>): Map<String, Any> {
    val result = inputParams.toMutableMap()

    for (paramSpec in inputs) {
      val paramName = paramSpec.name

      if (paramSpec.secret && result.containsKey(paramName)) {
        val value = result[paramName]
        if (value != null) {
          result[paramName] = redactValue(value.toString())
        }
      }
    }

    return result
  }

  private fun redactValue(value: String): String {
    return if (value.length <= 4) {
      "***"
    } else {
      "${value.substring(0, 2)}***${value.substring(value.length - 2)}"
    }
  }

  /**
   * Generates JSON Schema representation of this tool specification.
   *
   * @return JSON Schema as JsonNode
   */
  fun toJsonSchema(): JsonNode {
    val schema = OBJECT_MAPPER.createObjectNode()

    // Basic schema metadata
    schema.put("\$schema", "http://json-schema.org/draft-07/schema#")
    schema.put("type", "object")
    schema.put("title", name)
    description?.let { schema.put("description", it) }

    // Add tool metadata
    val toolInfo = schema.putObject("tool")
    toolInfo.put("name", name)
    toolInfo.put("version", version)
    description?.let { toolInfo.put("description", it) }

    // Add custom metadata
    if (metadata.isNotEmpty()) {
      val metadataNode = toolInfo.putObject("metadata")
      metadata.forEach { (key, value) ->
        metadataNode.set<JsonNode>(key, OBJECT_MAPPER.valueToTree(value))
      }
    }

    // Input parameters schema
    if (inputs.isNotEmpty()) {
      val inputsSchema = schema.putObject("inputs")
      inputsSchema.put("type", "object")

      val properties = inputsSchema.putObject("properties")
      val required = inputsSchema.putArray("required")

      for (paramSpec in inputs) {
        val paramSchema = createParameterSchema(paramSpec)
        properties.set<JsonNode>(paramSpec.name, paramSchema)

        if (paramSpec.required) {
          required.add(paramSpec.name)
        }
      }

      // Add XOR groups as anyOf constraints
      if (xorGroups.isNotEmpty()) {
        val anyOf = inputsSchema.putArray("anyOf")
        for (xorGroup in xorGroups) {
          val xorSchema = OBJECT_MAPPER.createObjectNode()
          val oneOf = xorSchema.putArray("oneOf")

          for (paramName in xorGroup) {
            val paramRequired = OBJECT_MAPPER.createObjectNode()
            val requiredArray = paramRequired.putArray("required")
            requiredArray.add(paramName)
            oneOf.add(paramRequired)
          }

          anyOf.add(xorSchema)
        }
      }
    }

    // Output parameters schema
    if (outputs.isNotEmpty()) {
      val outputsSchema = schema.putObject("outputs")
      outputsSchema.put("type", "object")

      val properties = outputsSchema.putObject("properties")
      val required = outputsSchema.putArray("required")

      for (paramSpec in outputs) {
        val paramSchema = createParameterSchema(paramSpec)
        properties.set<JsonNode>(paramSpec.name, paramSchema)

        if (paramSpec.required) {
          required.add(paramSpec.name)
        }
      }
    }

    return schema
  }

  /**
   * Returns the set of input parameter names for capability matching.
   */
  fun getInputKeys(): Set<String> = inputs.map { it.name }.toSet()

  /**
   * Returns the set of output parameter names for capability matching.
   */
  fun getOutputKeys(): Set<String> = outputs.map { it.name }.toSet()

  /**
   * Creates a JSON Schema for a single parameter.
   */
  private fun createParameterSchema(paramSpec: ParamSpec<*>): ObjectNode {
    val paramSchema = OBJECT_MAPPER.createObjectNode()
    val rawClass = paramSpec.type.rawClass

    // Determine JSON Schema type
    when {
      String::class.java == rawClass -> paramSchema.put("type", "string")
      Int::class.javaObjectType == rawClass || Long::class.javaObjectType == rawClass ->
        paramSchema.put("type", "integer")

      Double::class.javaObjectType == rawClass || Float::class.javaObjectType == rawClass ->
        paramSchema.put("type", "number")

      Boolean::class.javaObjectType == rawClass -> paramSchema.put("type", "boolean")
      List::class.java.isAssignableFrom(rawClass) -> paramSchema.put("type", "array")
      Map::class.java.isAssignableFrom(rawClass) -> paramSchema.put("type", "object")
      else -> paramSchema.put("type", "object")
    }

    // Add constraints
    paramSpec.min?.let { paramSchema.put("minimum", it.toDouble()) }
    paramSpec.max?.let { paramSchema.put("maximum", it.toDouble()) }
    paramSpec.regex?.let { paramSchema.put("pattern", it) }

    if (!paramSpec.enumValues.isNullOrEmpty()) {
      val enumArray = paramSchema.putArray("enum")
      for (enumValue in paramSpec.enumValues) {
        enumArray.add(OBJECT_MAPPER.valueToTree(enumValue) as JsonNode)
      }
    }

    paramSpec.defaultValue?.let {
      paramSchema.set<JsonNode>("default", OBJECT_MAPPER.valueToTree(it))
    }

    paramSpec.doc?.let { paramSchema.put("description", it) }

    // Add custom properties
    if (paramSpec.secret) {
      paramSchema.put("secret", true)
    }
    if (paramSpec.adjustable) {
      paramSchema.put("adjustable", true)
    }

    return paramSchema
  }

}