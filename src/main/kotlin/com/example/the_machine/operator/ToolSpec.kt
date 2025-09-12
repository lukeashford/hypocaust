package com.example.the_machine.operator

import com.example.the_machine.operator.result.ValidationResult
import kotlinx.serialization.json.*

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
   * @return JSON Schema as JsonElement
   */
  fun toJsonSchema(): JsonElement {
    return buildJsonObject {
      // Basic schema metadata
      put("\$schema", "http://json-schema.org/draft-07/schema#")
      put("type", "object")
      put("title", name)
      description?.let { put("description", it) }

      // Add tool metadata
      put("tool", buildJsonObject {
        put("name", name)
        put("version", version)
        description?.let { put("description", it) }

        // Add custom metadata
        if (metadata.isNotEmpty()) {
          put("metadata", buildJsonObject {
            metadata.forEach { (key, value) ->
              when (value) {
                is String -> put(key, value)
                is Number -> put(key, value)
                is Boolean -> put(key, value)
                else -> put(key, value.toString())
              }
            }
          })
        }
      })

      // Input parameters schema
      if (inputs.isNotEmpty()) {
        put("inputs", buildJsonObject {
          put("type", "object")

          put("properties", buildJsonObject {
            for (paramSpec in inputs) {
              put(paramSpec.name, createParameterSchema(paramSpec))
            }
          })

          val requiredParams = inputs.filter { it.required }.map { it.name }
          if (requiredParams.isNotEmpty()) {
            put("required", buildJsonArray {
              requiredParams.forEach { add(it) }
            })
          }

          // Add XOR groups as anyOf constraints
          if (xorGroups.isNotEmpty()) {
            put("anyOf", buildJsonArray {
              for (xorGroup in xorGroups) {
                add(buildJsonObject {
                  put("oneOf", buildJsonArray {
                    for (paramName in xorGroup) {
                      add(buildJsonObject {
                        put("required", buildJsonArray {
                          add(paramName)
                        })
                      })
                    }
                  })
                })
              }
            })
          }
        })
      }

      // Output parameters schema
      if (outputs.isNotEmpty()) {
        put("outputs", buildJsonObject {
          put("type", "object")

          put("properties", buildJsonObject {
            for (paramSpec in outputs) {
              put(paramSpec.name, createParameterSchema(paramSpec))
            }
          })

          val requiredParams = outputs.filter { it.required }.map { it.name }
          if (requiredParams.isNotEmpty()) {
            put("required", buildJsonArray {
              requiredParams.forEach { add(it) }
            })
          }
        })
      }
    }
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
  private fun createParameterSchema(paramSpec: ParamSpec<*>): JsonElement = buildJsonObject {
    val rawClass = paramSpec.type.rawClass

    put("type", getJsonSchemaType(rawClass))

    // Add numeric constraints  
    paramSpec.min?.let { put("minimum", formatNumericValue(it, rawClass)) }
    paramSpec.max?.let { put("maximum", formatNumericValue(it, rawClass)) }

    // Add string constraints
    paramSpec.regex?.let { put("pattern", it) }

    // Add enum constraints
    if (!paramSpec.enumValues.isNullOrEmpty()) {
      put("enum", buildJsonArray {
        paramSpec.enumValues.forEach { enumValue ->
          when (enumValue) {
            is String -> add(enumValue)
            is Number -> add(enumValue)
            is Boolean -> add(enumValue)
            else -> add(enumValue.toString())
          }
        }
      })
    }

    // Add default value
    paramSpec.defaultValue?.let { defaultValue ->
      when (defaultValue) {
        is String -> put("default", defaultValue)
        is Number -> put("default", defaultValue)
        is Boolean -> put("default", defaultValue)
        else -> put("default", defaultValue.toString())
      }
    }

    // Add documentation
    paramSpec.doc?.let { put("description", it) }

    // Add custom properties
    if (paramSpec.secret) put("secret", true)
    if (paramSpec.adjustable) put("adjustable", true)
  }

  private fun getJsonSchemaType(rawClass: Class<*>): String = when {
    rawClass == String::class.java -> "string"
    isIntegerType(rawClass) -> "integer"
    isFloatingPointType(rawClass) -> "number"
    isBooleanType(rawClass) -> "boolean"
    List::class.java.isAssignableFrom(rawClass) -> "array"
    Map::class.java.isAssignableFrom(rawClass) -> "object"
    else -> "object"
  }

  private fun formatNumericValue(value: Number, rawClass: Class<*>): Number =
    if (isIntegerType(rawClass)) value.toLong() else value.toDouble()

  private fun isIntegerType(rawClass: Class<*>): Boolean =
    rawClass in setOf(
      Int::class.java,
      Int::class.javaObjectType,
      Long::class.java,
      Long::class.javaObjectType
    )

  private fun isFloatingPointType(rawClass: Class<*>): Boolean =
    rawClass in setOf(
      Double::class.java,
      Double::class.javaObjectType,
      Float::class.java,
      Float::class.javaObjectType
    )

  private fun isBooleanType(rawClass: Class<*>): Boolean =
    rawClass in setOf(Boolean::class.java, Boolean::class.javaObjectType)

}