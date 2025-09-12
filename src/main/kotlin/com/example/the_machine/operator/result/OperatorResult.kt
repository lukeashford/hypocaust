package com.example.the_machine.operator.result

import kotlinx.serialization.json.JsonElement

/**
 * Result type for operator execution operations. Extends BaseResult with operator-specific
 * fields and factory methods for common operator execution scenarios.
 */
class OperatorResult private constructor(
  ok: Boolean,
  message: String,
  /**
   * Result code for this operation.
   */
  val code: OperatorResultCode,
  /**
   * Normalized inputs that were provided to the operator.
   */
  normalizedInputs: Map<String, Any>?,
  /**
   * Outputs produced by the operator.
   */
  outputs: Map<String, Any>?,
  /**
   * Metrics collected during operator execution.
   */
  metrics: Map<String, Any>?,
  /**
   * Remediation patches that may be applied to fix issues.
   */
  remediationPatches: List<JsonElement>?,
  /**
   * Name of the operator that produced this result.
   */
  val operatorName: String?,
  /**
   * Version of the operator that produced this result.
   */
  val operatorVersion: String?,
  /**
   * Number of attempts made to execute this operator.
   */
  val attempts: Int
) : BaseResult(ok, message) {

  val normalizedInputs: Map<String, Any> = normalizedInputs ?: emptyMap()
  val outputs: Map<String, Any> = outputs ?: emptyMap()
  val metrics: Map<String, Any> = metrics ?: emptyMap()
  val remediationPatches: List<JsonElement> = remediationPatches ?: emptyList()

  companion object {

    /**
     * Factory method for successful operator results.
     */
    fun success(): OperatorResult = OperatorResult(
      ok = true,
      message = "Operation completed successfully",
      code = OperatorResultCode.SUCCESS,
      normalizedInputs = emptyMap(),
      outputs = emptyMap(),
      metrics = emptyMap(),
      remediationPatches = emptyList(),
      operatorName = "",
      operatorVersion = "",
      attempts = 1
    )

    /**
     * Factory method for successful operator results with custom message.
     */
    fun success(message: String): OperatorResult = OperatorResult(
      ok = true,
      message = message,
      code = OperatorResultCode.SUCCESS,
      normalizedInputs = emptyMap(),
      outputs = emptyMap(),
      metrics = emptyMap(),
      remediationPatches = emptyList(),
      operatorName = "",
      operatorVersion = "",
      attempts = 1
    )

    /**
     * Factory method for successful operator results with full details.
     */
    fun success(
      operatorName: String,
      operatorVersion: String,
      normalizedInputs: Map<String, Any>,
      outputs: Map<String, Any>
    ): OperatorResult = OperatorResult(
      ok = true,
      message = "Operation completed successfully",
      code = OperatorResultCode.SUCCESS,
      normalizedInputs = normalizedInputs,
      outputs = outputs,
      metrics = emptyMap(),
      remediationPatches = emptyList(),
      operatorName = operatorName,
      operatorVersion = operatorVersion,
      attempts = 1
    )

    /**
     * Factory method for successful operator results with custom message and full details.
     */
    fun success(
      operatorName: String,
      operatorVersion: String,
      message: String,
      normalizedInputs: Map<String, Any>,
      outputs: Map<String, Any>
    ): OperatorResult = OperatorResult(
      ok = true,
      message = message,
      code = OperatorResultCode.SUCCESS,
      normalizedInputs = normalizedInputs,
      outputs = outputs,
      metrics = emptyMap(),
      remediationPatches = emptyList(),
      operatorName = operatorName,
      operatorVersion = operatorVersion,
      attempts = 1
    )

    /**
     * Factory method for failed operator results.
     */
    fun failure(
      operatorName: String,
      operatorVersion: String,
      code: OperatorResultCode,
      message: String,
      normalizedInputs: Map<String, Any>
    ): OperatorResult = OperatorResult(
      ok = false,
      message = message,
      code = code,
      normalizedInputs = normalizedInputs,
      outputs = emptyMap(),
      metrics = emptyMap(),
      remediationPatches = emptyList(),
      operatorName = operatorName,
      operatorVersion = operatorVersion,
      attempts = 1
    )

    /**
     * Factory method for validation failure results.
     */
    fun validationFailure(
      operatorName: String,
      operatorVersion: String,
      validationMessage: String
    ): OperatorResult = OperatorResult(
      ok = false,
      message = validationMessage,
      code = OperatorResultCode.VALIDATION_ERROR,
      normalizedInputs = emptyMap(),
      outputs = emptyMap(),
      metrics = emptyMap(),
      remediationPatches = emptyList(),
      operatorName = operatorName,
      operatorVersion = operatorVersion,
      attempts = 1
    )
  }

  /**
   * Returns a copy of this result with updated attempt count.
   */
  fun withAttempts(newAttempts: Int): OperatorResult = OperatorResult(
    ok = ok,
    message = message,
    code = code,
    normalizedInputs = normalizedInputs,
    outputs = outputs,
    metrics = metrics,
    remediationPatches = remediationPatches,
    operatorName = operatorName,
    operatorVersion = operatorVersion,
    attempts = newAttempts
  )

  /**
   * Returns a copy of this result with additional metrics.
   */
  fun withMetrics(additionalMetrics: Map<String, Any>): OperatorResult = OperatorResult(
    ok = ok,
    message = message,
    code = code,
    normalizedInputs = normalizedInputs,
    outputs = outputs,
    metrics = metrics + additionalMetrics,
    remediationPatches = remediationPatches,
    operatorName = operatorName,
    operatorVersion = operatorVersion,
    attempts = attempts
  )

  /**
   * Returns a copy of this result with remediation patches.
   */
  fun withRemediationPatches(patches: List<JsonElement>): OperatorResult = OperatorResult(
    ok = ok,
    message = message,
    code = code,
    normalizedInputs = normalizedInputs,
    outputs = outputs,
    metrics = metrics,
    remediationPatches = patches,
    operatorName = operatorName,
    operatorVersion = operatorVersion,
    attempts = attempts
  )

  override fun toString(): String =
    "OperatorResult{ok=$ok, message='$message', code=$code, operatorName='$operatorName', operatorVersion='$operatorVersion', attempts=$attempts}"
}