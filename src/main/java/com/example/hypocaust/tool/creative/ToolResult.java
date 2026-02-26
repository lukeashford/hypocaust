package com.example.hypocaust.tool.creative;

/**
 * Common contract for all tool execution results. The decomposer reads the serialized JSON
 * to decide whether to retry, adjust, or give up.
 */
public sealed interface ToolResult
    permits GenerateCreativeResult, DeleteResult, RestoreResult {

  boolean success();

  String summary();

  String error();
}
