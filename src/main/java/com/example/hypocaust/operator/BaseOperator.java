package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.OperatorResult;
import java.util.Map;

public abstract class BaseOperator implements Operator {

  public final OperatorResult execute(Map<String, Object> rawInputs) {
    try {
      // First normalize (apply defaults), then validate
      final var normalizedInputs = spec().normalize(rawInputs);

      final var validationResult = spec().validate(normalizedInputs);
      if (!validationResult.ok()) {
        return OperatorResult.failure(validationResult.message(), normalizedInputs);
      }

      return doExecute(normalizedInputs);
    } catch (Exception e) {
      return OperatorResult.failure(e.getMessage(), rawInputs);
    }
  }

  public String getName() {
    return spec().name();
  }

  protected abstract OperatorResult doExecute(Map<String, Object> normalizedInputs);
}
