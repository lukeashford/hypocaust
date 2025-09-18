package com.example.the_machine.domain.event;

import java.util.List;
import java.util.Map;

public record OperatorLedger(
    Map<String, Object> values,
    List<ChildConfig> children
) {

  public record ChildConfig(
      String operatorName,
      List<String> inputNames,
      String outputName
  ) {

  }

}
