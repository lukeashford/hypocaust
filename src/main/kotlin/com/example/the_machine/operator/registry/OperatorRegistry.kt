package com.example.the_machine.operator.registry

import com.example.the_machine.operator.Operator
import com.example.the_machine.operator.ToolSpec

/**
 * Registry interface for discovering and searching operators by name and semantic task matching.
 * Designed for LLM-based recursive problem solving where operators are discovered through semantic
 * similarity rather than rigid parameter matching.
 */
interface OperatorRegistry {

  /**
   * Finds an operator by its exact name.
   *
   * @param name the operator name to search for
   * @return the operator instance if found, null otherwise
   */
  fun get(name: String): Operator?

  /**
   * Searches for operators that can handle the given task description using semantic similarity.
   * This is the primary method for discovering operators in LLM-based problem solving.
   *
   * @param taskDescription the task description to search for
   * @return list of tool specs ordered by similarity
   */
  fun searchByTask(taskDescription: String): List<ToolSpec>

  /**
   * Returns the number of operators currently registered.
   *
   * @return the total count of operators
   */
  fun size(): Int
}