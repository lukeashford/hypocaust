package com.example.hypocaust.domain;

import java.util.List;
import java.util.Map;

public record OperatorLedger(
    Map<String, Object> values,
    List<ChildConfig> children,
    String finalOutputKey
) {

  public record ChildConfig(
      String operatorName,
      Map<String, String> inputsToKeys,
      Map<String, String> outputsToKeys
  ) {

  }

}
