package com.example.hypocaust.tool.creative;

/**
 * Common contract for all tool execution results. Provides a uniform way for upstream layers
 * (e.g., the decomposer) to inspect what happened during tool execution without needing to know
 * the concrete tool type.
 *
 * <p>The {@link #report()} method exposes a structured {@link ExecutionReport} containing
 * attempt-level metadata — which models were tried, what errors occurred, which artifacts were
 * produced — enabling the decomposer to make informed self-healing decisions rather than
 * re-trying blindly.
 */
public sealed interface ToolResult
    permits GenerateCreativeResult, DeleteResult, RestoreResult {

  boolean success();

  String summary();

  String error();

  ExecutionReport report();
}
