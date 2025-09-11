package com.example.the_machine.operator

import com.example.the_machine.operator.result.ValidationResult
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory
import java.util.regex.Pattern

/**
 * Specification for a parameter with validation rules and metadata. Supports builder pattern for
 * flexible construction and validation.
 *
 * @param T the Java type of the parameter
 */
data class ParamSpec<T>(
  val name: String,
  val type: JavaType,
  val required: Boolean = false,
  val defaultValue: T? = null,
  val min: Number? = null,
  val max: Number? = null,
  val enumValues: List<T>? = null,
  val regex: String? = null,
  val secret: Boolean = false,
  val adjustable: Boolean = true,
  val doc: String? = null
) {

  // Compiled regex pattern for validation - lazy initialization
  @Transient
  private var compiledRegex: Pattern? = null

  /**
   * Validates a value against this parameter specification.
   *
   * @param value the value to validate
   * @return validation result with error message if invalid
   */
  fun validate(value: Any?): ValidationResult {
    // Check required constraint
    if (required && value == null) {
      return ValidationResult.parameterValidationError(
        name, null,
        "Parameter '$name' is required but was null"
      )
    }

    if (value == null) {
      return ValidationResult.success("Parameter '$name' validation completed successfully")
    }

    // Check type compatibility
    @Suppress("UNCHECKED_CAST")
    val castValue = if (type.rawClass.isInstance(value)) value as T else null
    if (castValue == null) {
      return ValidationResult.parameterValidationError(
        name, value,
        "Parameter '$name' expected type ${type.rawClass.simpleName} but got ${value::class.simpleName}"
      )
    }

    // Validate numeric constraints
    if (castValue is Number && (min != null || max != null)) {
      val result = validateNumericConstraints(castValue)
      if (!result.ok) {
        return result
      }
    }

    // Validate enum constraints
    if (!enumValues.isNullOrEmpty()) {
      if (castValue !in enumValues) {
        return ValidationResult.parameterValidationError(
          name, castValue,
          "Parameter '$name' must be one of $enumValues but was $castValue"
        )
      }
    }

    // Validate regex constraints
    if (regex != null && castValue is String) {
      val result = validateRegexConstraint(castValue)
      if (!result.ok) {
        return result
      }
    }

    return ValidationResult.success("Parameter '$name' validation completed successfully")
  }

  private fun validateNumericConstraints(value: Number): ValidationResult {
    val doubleValue = value.toDouble()

    min?.let { minVal ->
      if (doubleValue < minVal.toDouble()) {
        return ValidationResult.constraintValidationError(
          name, "MIN_VALUE",
          "Parameter '$name' must be >= $minVal but was $value"
        )
      }
    }

    max?.let { maxVal ->
      if (doubleValue > maxVal.toDouble()) {
        return ValidationResult.constraintValidationError(
          name, "MAX_VALUE",
          "Parameter '$name' must be <= $maxVal but was $value"
        )
      }
    }

    return ValidationResult.success("Numeric constraints validated for parameter '$name'")
  }

  private fun validateRegexConstraint(value: String): ValidationResult {
    if (compiledRegex == null) {
      try {
        compiledRegex = regex?.let { Pattern.compile(it) }
      } catch (e: Exception) {
        return ValidationResult.constraintValidationError(
          name, "INVALID_REGEX",
          "Parameter '$name' has invalid regex pattern: $regex"
        )
      }
    }

    if (!compiledRegex!!.matcher(value).matches()) {
      return ValidationResult.constraintValidationError(
        name, "REGEX_MISMATCH",
        "Parameter '$name' must match pattern '$regex' but was '$value'"
      )
    }

    return ValidationResult.success("Regex constraint validated for parameter '$name'")
  }

  companion object {
    // Factory methods for common parameter types

    /**
     * Creates a string parameter specification.
     */
    fun string(name: String): ParamSpec<String> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = false,
      secret = false,
      adjustable = true
    )

    /**
     * Creates a string parameter specification with required flag and documentation.
     */
    fun string(name: String, required: Boolean, doc: String): ParamSpec<String> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = required,
      doc = doc,
      secret = false,
      adjustable = true
    )

    /**
     * Creates an integer parameter specification.
     */
    fun integer(name: String): ParamSpec<Int> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(Int::class.javaObjectType),
      required = false,
      secret = false,
      adjustable = true
    )

    /**
     * Creates a long parameter specification.
     */
    fun longParam(name: String): ParamSpec<Long> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(Long::class.javaObjectType),
      required = false,
      secret = false,
      adjustable = true
    )

    /**
     * Creates a double parameter specification.
     */
    fun doubleParam(name: String): ParamSpec<Double> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(Double::class.javaObjectType),
      required = false,
      secret = false,
      adjustable = true
    )

    /**
     * Creates a boolean parameter specification.
     */
    fun bool(name: String): ParamSpec<Boolean> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(Boolean::class.javaObjectType),
      required = false,
      secret = false,
      adjustable = true
    )

    /**
     * Creates an enumeration parameter specification.
     */
    fun <T> enumeration(name: String, enumClass: Class<T>, values: List<T>): ParamSpec<T> =
      ParamSpec(
        name = name,
        type = TypeFactory.defaultInstance().constructType(enumClass),
        enumValues = values,
        required = false,
        secret = false,
        adjustable = true
      )

    /**
     * Creates a secret parameter specification (like API keys, passwords).
     */
    fun secret(name: String): ParamSpec<String> = ParamSpec(
      name = name,
      type = TypeFactory.defaultInstance().constructType(String::class.java),
      required = false,
      secret = true,
      adjustable = false // secrets typically shouldn't be auto-adjusted
    )

    /**
     * Creates a list parameter specification.
     */
    fun <T> list(name: String, elementType: Class<T>): ParamSpec<List<T>> {
      val listType =
        TypeFactory.defaultInstance().constructCollectionType(List::class.java, elementType)
      return ParamSpec(
        name = name,
        type = listType,
        required = false,
        secret = false,
        adjustable = true
      )
    }

    /**
     * Creates a map parameter specification.
     */
    fun <V> map(name: String, valueType: Class<V>): ParamSpec<Map<String, V>> {
      val mapType = TypeFactory.defaultInstance()
        .constructMapType(Map::class.java, String::class.java, valueType)
      return ParamSpec(
        name = name,
        type = mapType,
        required = false,
        secret = false,
        adjustable = true
      )
    }
  }
}