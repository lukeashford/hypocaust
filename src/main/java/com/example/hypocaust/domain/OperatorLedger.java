package com.example.hypocaust.domain;

import java.util.List;
import java.util.Map;

/**
 * Ledger for operator chain execution.
 *
 * @param values         Shared values map for passing data between operators
 * @param children       List of child operator configurations
 * @param finalOutputKey Key in values map for the final output
 */
public record OperatorLedger(
    Map<String, Object> values,
    List<ChildConfig> children,
    String finalOutputKey
) {

  /**
   * Configuration for a child operator in the chain.
   *
   * @param operatorName   Name of the operator to invoke
   * @param todo           Human-readable task description (e.g., "Generate hero portrait image")
   * @param inputsToKeys   Mapping of operator input names to ledger value keys
   * @param outputsToKeys  Mapping of operator output names to ledger value keys
   */
  public record ChildConfig(
      String operatorName,
      String todo,
      Map<String, String> inputsToKeys,
      Map<String, String> outputsToKeys
  ) {

    /**
     * Legacy constructor without todo field for backwards compatibility.
     */
    public ChildConfig(String operatorName, Map<String, String> inputsToKeys, Map<String, String> outputsToKeys) {
      this(operatorName, null, inputsToKeys, outputsToKeys);
    }
  }
}
