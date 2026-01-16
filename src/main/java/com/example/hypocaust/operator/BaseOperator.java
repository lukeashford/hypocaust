package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.OperatorResult;
import java.util.Map;

public abstract class BaseOperator implements Operator {

  public final OperatorResult execute(Map<String, Object> rawInputs) {
    try {
      final var validationResult = spec().validate(rawInputs);
      if (!validationResult.ok()) {
        return OperatorResult.failure(validationResult.message(), rawInputs);
      }

      return doExecute(rawInputs);
    } catch (Exception e) {
      return OperatorResult.failure(e.getMessage(), rawInputs);
    }
  }

  public String getName() {
    return spec().name();
  }

  protected abstract OperatorResult doExecute(Map<String, Object> normalizedInputs);

}
