package com.example.the_machine.operator.result

/**
 * Result type specifically for validation operations. Extends BaseResult with validation-specific
 * fields and factory methods for common validation scenarios.
 */
class ValidationResult private constructor(
  ok: Boolean,
  message: String,
  /**
   * Type of validation that was performed (e.g., "PARAMETER", "CONSTRAINT", "XOR_GROUP").
   */
  val validationType: String?,
  /**
   * Name of the field that failed validation (optional).
   */
  val fieldName: String?,
  /**
   * The value that was rejected during validation (optional).
   */
  val rejectedValue: Any?
) : BaseResult(ok, message) {

  companion object {

    /**
     * Factory method for successful validation results.
     */
    fun success(): ValidationResult =
      ValidationResult(true, "Validation completed successfully", null, null, null)

    /**
     * Factory method for successful validation results with custom message.
     */
    fun success(message: String): ValidationResult =
      ValidationResult(true, message, null, null, null)

    /**
     * Factory method for validation error results.
     */
    fun error(message: String): ValidationResult =
      ValidationResult(false, message, "ERROR", null, null)

    /**
     * Factory method for parameter validation errors.
     */
    fun parameterValidationError(
      fieldName: String,
      rejectedValue: Any?,
      message: String
    ): ValidationResult = ValidationResult(false, message, "PARAMETER", fieldName, rejectedValue)

    /**
     * Factory method for constraint validation errors.
     */
    fun constraintValidationError(
      fieldName: String,
      constraintType: String,
      message: String
    ): ValidationResult = ValidationResult(false, message, "CONSTRAINT", fieldName, constraintType)

    /**
     * Factory method for XOR group validation errors.
     */
    fun xorGroupValidationError(groupFields: List<String>?, message: String): ValidationResult =
      ValidationResult(
        false,
        message,
        "XOR_GROUP",
        groupFields?.joinToString(", "),
        null
      )
  }

  override fun toString(): String = buildString {
    append("ValidationResult{ok=").append(ok)
      .append(", message='").append(message).append("'")

    validationType?.let { append(", validationType='").append(it).append("'") }
    fieldName?.let { append(", fieldName='").append(it).append("'") }
    rejectedValue?.let { append(", rejectedValue=").append(it) }

    append("}")
  }
}