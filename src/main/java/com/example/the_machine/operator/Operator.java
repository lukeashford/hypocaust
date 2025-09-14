package com.example.the_machine.operator;

import com.example.the_machine.dto.RunDto;
import com.example.the_machine.operator.result.OperatorResult;
import java.util.Map;

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
  ToolSpec spec();

  /**
   * Executes the operator with the provided context and input parameters. The inputs should be
   * validated against the spec before calling this method, but implementations should be defensive
   * and handle invalid inputs gracefully.
   *
   * @param ctx the execution context containing repositories, services, and utilities
   * @param rawInputs the input parameters (may need validation and normalization)
   * @return the result of the operation including outputs, metrics, and status
   * @throws Exception if the operation fails catastrophically
   */
  OperatorResult execute(RunDto ctx, Map<String, Object> rawInputs) throws Exception;
}