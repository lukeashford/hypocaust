package com.example.hypocaust.operator.registry;

import com.example.hypocaust.operator.Operator;
import com.example.hypocaust.operator.OperatorSpec;
import java.util.List;
import java.util.Optional;

/**
 * Registry interface for discovering and searching operators by fileName and semantic task
 * matching. Designed for LLM-based recursive problem solving where operators are discovered through
 * semantic similarity rather than rigid parameter matching.
 */
public interface OperatorRegistry {

  /**
   * Finds an operator by its exact fileName.
   *
   * @param name the operator fileName to search for
   * @return the operator instance if found, empty otherwise
   */
  Optional<Operator> get(String name);

  /**
   * Searches for operators that can handle the given task description using semantic similarity.
   * This is the primary method for discovering operators in LLM-based problem solving.
   *
   * @param taskDescription the task description to search for
   * @return list of tool specs ordered by similarity
   */
  List<OperatorSpec> searchByTask(String taskDescription);

  /**
   * Returns the number of operators currently registered.
   *
   * @return the total count of operators
   */
  int size();
}