package com.example.the_machine.operator;

import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.operator.result.OperatorResultCode;
import java.util.Map;

public abstract class BaseOperator implements Operator {

  public final OperatorResult execute(Map<String, Object> rawInputs) {
    try {
      final var validationResult = spec().validate(rawInputs);
      if (!validationResult.isOk()) {
        return OperatorResult.validationFailure(this.getClass().getSimpleName(),
            getVersionString(), validationResult.getMessage());
      }

      final var normalizedInputs = spec().applyDefaults(rawInputs);

      return doExecute(normalizedInputs);
    } catch (Exception e) {
      return OperatorResult.failure(
          getName(),
          getVersionString(),
          OperatorResultCode.UNEXPECTED_ERROR,
          e.getMessage(),
          rawInputs
      );
    }
  }

  public String getName() {
    return spec().getName();
  }

  public final String getVersionString() {
    return spec().getVersion().toString();
  }

  protected abstract OperatorResult doExecute(Map<String, Object> normalizedInputs);

}
