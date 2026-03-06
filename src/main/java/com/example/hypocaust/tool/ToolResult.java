package com.example.hypocaust.tool;

/**
 * Common contract for all tool execution results. The decomposer reads the serialized JSON to
 * decide whether to retry, adjust, or give up.
 */
public interface ToolResult {

  default boolean success() {
    return error() == null;
  }

  String summary();

  String error();
}
