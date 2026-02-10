package com.example.hypocaust.operator;

import com.example.hypocaust.operator.result.OperatorResult;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for operators that perform units of work within the system. Each operator declares its
 * contract via a ToolSpec and can be executed with validated inputs to produce structured results.
 */
public interface Operator {

  /**
   * Returns the specification for this operator, including input/output parameters, validation
   * rules, and metadata.
   *
   * @return the tool specification
   */
  OperatorSpec spec();

  /**
   * Executes the operator with the provided context, input parameters and a todo ID.
   *
   * @param rawInputs the input parameters
   * @param todoId the ID of the todo in the task tree
   * @return the result of the operation
   */
  OperatorResult execute(Map<String, Object> rawInputs, UUID todoId);
}