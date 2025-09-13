package com.example.the_machine.operator.result;

import java.util.List;
import lombok.Getter;

/**
 * Result type specifically for validation operations. Extends BaseResult with validation-specific
 * fields and factory methods for common validation scenarios.
 */
@Getter
public class ValidationResult extends BaseResult {

  /**
   * Type of validation that was performed (e.g., "PARAMETER", "CONSTRAINT", "XOR_GROUP").
   */
  private final String validationType;

  /**
   * Name of the field that failed validation (optional).
   */
  private final String fieldName;

  /**
   * The value that was rejected during validation (optional).
   */
  private final Object rejectedValue;

  /**
   * Constructor for ValidationResult.
   */
  private ValidationResult(boolean ok, String message, String validationType, String fieldName,
      Object rejectedValue) {
    super(ok, message);
    this.validationType = validationType;
    this.fieldName = fieldName;
    this.rejectedValue = rejectedValue;
  }

  /**
   * Factory method for successful validation results.
   *
   * @return a successful ValidationResult
   */
  public static ValidationResult success() {
    return createSuccess("Validation completed successfully", (ok, msg) ->
        new ValidationResult(ok, msg, null, null, null));
  }

  /**
   * Factory method for successful validation results with custom message.
   *
   * @param message custom success message
   * @return a successful ValidationResult
   */
  public static ValidationResult success(String message) {
    return createSuccess(message, (ok, msg) ->
        new ValidationResult(ok, msg, null, null, null));
  }

  /**
   * Factory method for validation error results.
   *
   * @param message error message
   * @return a failed ValidationResult
   */
  public static ValidationResult error(String message) {
    return createFailure(message, (ok, msg) ->
        new ValidationResult(ok, msg, "ERROR", null, null));
  }

  /**
   * Factory method for parameter validation errors.
   *
   * @param fieldName name of the field that failed validation
   * @param rejectedValue the value that was rejected
   * @param message validation error message
   * @return a failed ValidationResult with parameter validation details
   */
  public static ValidationResult parameterValidationError(String fieldName, Object rejectedValue,
      String message) {
    return new ValidationResult(false, message, "PARAMETER", fieldName, rejectedValue);
  }

  /**
   * Factory method for constraint validation errors.
   *
   * @param fieldName name of the field that failed validation
   * @param constraintType type of constraint that failed
   * @param message validation error message
   * @return a failed ValidationResult with constraint validation details
   */
  public static ValidationResult constraintValidationError(String fieldName, String constraintType,
      String message) {
    return new ValidationResult(false, message, "CONSTRAINT", fieldName, constraintType);
  }

  /**
   * Factory method for XOR group validation errors.
   *
   * @param groupFields list of field names in the XOR group
   * @param message validation error message
   * @return a failed ValidationResult with XOR group validation details
   */
  public static ValidationResult xorGroupValidationError(List<String> groupFields, String message) {
    return new ValidationResult(false, message, "XOR_GROUP",
        groupFields != null ? String.join(", ", groupFields) : null, null);
  }

  @Override
  public String toString() {
    final var sb = new StringBuilder();
    sb.append("ValidationResult{ok=")
        .append(isOk())
        .append(", message='")
        .append(getMessage())
        .append("'");

    if (validationType != null) {
      sb.append(", validationType='").append(validationType).append("'");
    }
    if (fieldName != null) {
      sb.append(", fieldName='").append(fieldName).append("'");
    }
    if (rejectedValue != null) {
      sb.append(", rejectedValue=").append(rejectedValue);
    }

    sb.append("}");
    return sb.toString();
  }
}