package com.example.the_machine.service

/**
 * Policy configuration for run execution limits and retry behavior.
 */
data class RunPolicy(
  val maxTriesPerOp: Int,
  val maxCostUsd: Double,
  val maxDepth: Int,
  val maxNodes: Int
) {

  init {
    require(maxTriesPerOp > 0) { "maxTriesPerOp must be positive" }
    require(maxCostUsd > 0) { "maxCostUsd must be positive" }
    require(maxDepth > 0) { "maxDepth must be positive" }
    require(maxNodes > 0) { "maxNodes must be positive" }
  }

  companion object {

    fun defaultPolicy(): RunPolicy {
      return RunPolicy(3, 10.0, 10, 100)
    }
  }
}