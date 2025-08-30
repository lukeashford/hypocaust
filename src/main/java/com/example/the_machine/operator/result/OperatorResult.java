package com.example.the_machine.operator.result;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/**
 * In-memory protocol for a single operator invocation result. Contains execution status, normalized
 * inputs, outputs, metrics, and remediation information. This is not a database entity - the run
 * engine will aggregate multiple OperatorResults and attach a summarized protocol to RunEntity when
 * the run completes/fails.
 */
@Getter
public class OperatorResult extends BaseResult {

  /**
   * Status/error code (e.g., "SUCCESS", "VALIDATION_ERROR", "TIMEOUT", "API_ERROR").
   */
  private final String code;

  /**
   * The input parameters after validation and normalization.
   */
  private final Map<String, Object> normalizedInputs;

  /**
   * The output values produced by the operator.
   */
  private final Map<String, Object> outputs;

  /**
   * Execution metrics (latency, retries, resource usage, etc.).
   */
  private final Map<String, Object> metrics;

  /**
   * JSON patches that could be applied to fix issues and retry. Each patch suggests modifications
   * to input parameters.
   */
  private final List<JsonNode> remediationPatches;

  /**
   * Name of the operator that produced this result.
   */
  private final String operatorName;

  /**
   * Version of the operator that produced this result.
   */
  private final String operatorVersion;

  /**
   * Number of attempts made to execute this operation.
   */
  private final int attempts;

  /**
   * Private constructor for builder pattern.
   */
  private OperatorResult(boolean ok, String message, String code,
      Map<String, Object> normalizedInputs,
      Map<String, Object> outputs, Map<String, Object> metrics,
      List<JsonNode> remediationPatches, String operatorName, String operatorVersion,
      int attempts) {
    super(ok, message);
    this.code = code;
    this.normalizedInputs = normalizedInputs != null ? normalizedInputs : new HashMap<>();
    this.outputs = outputs != null ? outputs : new HashMap<>();
    this.metrics = metrics != null ? metrics : new HashMap<>();
    this.remediationPatches = remediationPatches != null ? remediationPatches : List.of();
    this.operatorName = operatorName;
    this.operatorVersion = operatorVersion;
    this.attempts = attempts;
  }

  /**
   * Factory method for successful results from BaseResult.
   *
   * @return a successful OperatorResult
   */
  public static OperatorResult success() {
    return createSuccess("Operation completed successfully", (ok, msg) ->
        new OperatorResult(ok, msg, OperatorResultCode.SUCCESS.getValue(),
            new HashMap<>(), new HashMap<>(), new HashMap<>(), List.of(), null, null, 1));
  }

  /**
   * Factory method for successful results with custom message from BaseResult.
   *
   * @param message custom success message
   * @return a successful OperatorResult
   */
  public static OperatorResult success(String message) {
    return createSuccess(message, (ok, msg) ->
        new OperatorResult(ok, msg, OperatorResultCode.SUCCESS.getValue(),
            new HashMap<>(), new HashMap<>(), new HashMap<>(), List.of(), null, null, 1));
  }

  /**
   * Factory method for error results from BaseResult.
   *
   * @param message error message
   * @return a failed OperatorResult
   */
  public static OperatorResult error(String message) {
    return createFailure(message, (ok, msg) ->
        new OperatorResult(ok, msg, "ERROR",
            new HashMap<>(), new HashMap<>(), new HashMap<>(), List.of(), null, null, 1));
  }

  /**
   * Factory method for successful operation results.
   *
   * @param operatorName name of the operator
   * @param operatorVersion version of the operator
   * @param normalizedInputs the normalized input parameters
   * @param outputs the output values
   * @return a successful OperatorResult
   */
  public static OperatorResult success(
      String operatorName,
      String operatorVersion,
      Map<String, Object> normalizedInputs,
      Map<String, Object> outputs
  ) {
    return createSuccess("Operation completed successfully", (ok, msg) ->
        new OperatorResult(ok, msg, OperatorResultCode.SUCCESS.getValue(),
            normalizedInputs, outputs, new HashMap<>(), List.of(), operatorName, operatorVersion,
            1));
  }

  /**
   * Factory method for successful operation results with custom message.
   *
   * @param operatorName name of the operator
   * @param operatorVersion version of the operator
   * @param message custom success message
   * @param normalizedInputs the normalized input parameters
   * @param outputs the output values
   * @return a successful OperatorResult
   */
  public static OperatorResult success(
      String operatorName,
      String operatorVersion,
      String message,
      Map<String, Object> normalizedInputs,
      Map<String, Object> outputs
  ) {
    return createSuccess(message, (ok, msg) ->
        new OperatorResult(ok, msg, OperatorResultCode.SUCCESS.getValue(),
            normalizedInputs, outputs, new HashMap<>(), List.of(), operatorName, operatorVersion,
            1));
  }

  /**
   * Factory method for failed operation results.
   *
   * @param operatorName name of the operator
   * @param operatorVersion version of the operator
   * @param code error code
   * @param message error message
   * @param normalizedInputs the normalized input parameters (may be partial)
   * @return a failed OperatorResult
   */
  public static OperatorResult failure(
      String operatorName,
      String operatorVersion,
      String code, String message,
      Map<String, Object> normalizedInputs
  ) {
    return createFailure(message, (ok, msg) ->
        new OperatorResult(ok, msg, code,
            normalizedInputs, new HashMap<>(), new HashMap<>(), List.of(), operatorName,
            operatorVersion, 1));
  }

  /**
   * Factory method for validation failure results.
   *
   * @param operatorName name of the operator
   * @param operatorVersion version of the operator
   * @param validationMessage validation error message
   * @return a failed OperatorResult with validation error
   */
  public static OperatorResult validationFailure(
      String operatorName,
      String operatorVersion,
      String validationMessage
  ) {
    return createFailure(validationMessage, (ok, msg) ->
        new OperatorResult(ok, msg, OperatorResultCode.VALIDATION_ERROR.getValue(),
            new HashMap<>(), new HashMap<>(), new HashMap<>(), List.of(), operatorName,
            operatorVersion, 1));
  }

  /**
   * Factory method for exception-based failures.
   *
   * @param operatorName name of the operator
   * @param operatorVersion version of the operator
   * @param exception the exception that caused the failure
   * @param normalizedInputs the normalized input parameters
   * @return a failed OperatorResult
   */
  public static OperatorResult exception(
      String operatorName,
      String operatorVersion,
      Exception exception,
      Map<String, Object> normalizedInputs
  ) {
    String message = exception.getMessage() != null ? exception.getMessage()
        : exception.getClass().getSimpleName();
    return createFailure(message, (ok, msg) ->
        new OperatorResult(ok, msg, OperatorResultCode.EXCEPTION.getValue(),
            normalizedInputs, new HashMap<>(), new HashMap<>(), List.of(), operatorName,
            operatorVersion, 1));
  }

  /**
   * Creates a copy of this result with updated attempt count.
   *
   * @param newAttempts the new attempt count
   * @return a new OperatorResult with updated attempts
   */
  public OperatorResult withAttempts(int newAttempts) {
    return new OperatorResult(isOk(), getMessage(), code, normalizedInputs, outputs, metrics,
        remediationPatches, operatorName, operatorVersion, newAttempts);
  }

  /**
   * Creates a copy of this result with additional metrics.
   *
   * @param additionalMetrics metrics to merge in
   * @return a new OperatorResult with updated metrics
   */
  public OperatorResult withMetrics(Map<String, Object> additionalMetrics) {
    Map<String, Object> newMetrics = new HashMap<>(metrics);
    if (additionalMetrics != null) {
      newMetrics.putAll(additionalMetrics);
    }

    return new OperatorResult(isOk(), getMessage(), code, normalizedInputs, outputs, newMetrics,
        remediationPatches, operatorName, operatorVersion, attempts);
  }

  /**
   * Creates a copy of this result with remediation patches.
   *
   * @param patches remediation patches to apply
   * @return a new OperatorResult with patches
   */
  public OperatorResult withRemediationPatches(List<JsonNode> patches) {
    return new OperatorResult(isOk(), getMessage(), code, normalizedInputs, outputs, metrics,
        patches != null ? patches : List.of(), operatorName, operatorVersion, attempts);
  }

  @Override
  public String toString() {
    return String.format(
        "OperatorResult{ok=%s, code='%s', operator='%s@%s', attempts=%d, message='%s'}",
        isOk(), code, operatorName, operatorVersion, attempts, getMessage());
  }
}