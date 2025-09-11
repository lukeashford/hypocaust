package com.example.the_machine.operator.result

import kotlinx.serialization.Serializable

/**
 * Enum representing the common result codes for operator execution. Provides type-safe constants
 * for the most frequently used codes.
 */
@Serializable
enum class OperatorResultCode {

  SUCCESS,
  VALIDATION_ERROR,
  EXECUTION_FAILED,
  UNEXPECTED_ERROR,
  RETRY_LOOP_ERROR,
  BUDGET_EXCEEDED;

}