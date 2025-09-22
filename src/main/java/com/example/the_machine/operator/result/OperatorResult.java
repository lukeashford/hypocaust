package com.example.the_machine.operator.result;

import java.util.Map;

/**
 * Operator execution result that extends Result with input and output maps. Contains the inputs
 * that were processed and outputs that were produced.
 */
public record OperatorResult(
    boolean ok, String message,
    Map<String, Object> inputs,
    Map<String, Object> outputs
) {

  /**
   * Creates a successful OperatorResult with the given inputs and outputs.
   *
   * @param message success message
   * @param inputs input parameters that were processed
   * @param outputs output values that were produced
   * @return a successful OperatorResult
   */
  public static OperatorResult success(
      String message,
      Map<String, Object> inputs,
      Map<String, Object> outputs
  ) {
    return new OperatorResult(true, message != null ? message : "", inputs, outputs);
  }

  /**
   * Creates a failed OperatorResult with the given inputs.
   *
   * @param message failure message
   * @param inputs input parameters that were processed (may be partial)
   * @return a failed OperatorResult
   */
  public static OperatorResult failure(
      String message,
      Map<String, Object> inputs
  ) {
    return new OperatorResult(false, message != null ? message : "", inputs, Map.of());
  }
}