package com.example.the_machine.operator.result;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the common result codes for operator execution. Provides type-safe constants
 * for the most frequently used codes.
 */
public enum OperatorResultCode {
  SUCCESS, VALIDATION_ERROR, EXCEPTION;

  @JsonValue
  public String getValue() {
    return name();
  }
}