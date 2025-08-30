package com.example.the_machine.service;

/**
 * Policy configuration for run execution limits and retry behavior.
 */
public record RunPolicy(int maxTriesPerOp, double maxCostUsd, int maxDepth, int maxNodes) {

  public RunPolicy {
    if (maxTriesPerOp <= 0) {
      throw new IllegalArgumentException("maxTriesPerOp must be positive");
    }
    if (maxCostUsd <= 0) {
      throw new IllegalArgumentException("maxCostUsd must be positive");
    }
    if (maxDepth <= 0) {
      throw new IllegalArgumentException("maxDepth must be positive");
    }
    if (maxNodes <= 0) {
      throw new IllegalArgumentException("maxNodes must be positive");
    }
  }

  public static RunPolicy defaultPolicy() {
    return new RunPolicy(3, 10.0, 10, 100);
  }
}