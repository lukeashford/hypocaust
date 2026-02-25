package com.example.hypocaust.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result of executing a model, potentially after retrying transient failures. Contains the output
 * on success and a full log of each attempt with metadata (for propagation into
 * {@link com.example.hypocaust.tool.creative.ExecutionReport}).
 *
 * @param output the model output (null if all attempts failed)
 * @param attempts ordered list of attempt metadata; each map has at least "status" and may have
 *     "error", "retryReason", etc.
 */
public record ExecutionAttempt(
    JsonNode output,
    List<Map<String, String>> attempts
) {

  public boolean succeeded() {
    return output != null;
  }

  public String lastError() {
    if (attempts.isEmpty()) {
      return null;
    }
    return attempts.getLast().get("error");
  }

  public static ExecutionAttempt success(JsonNode output, List<Map<String, String>> attempts) {
    return new ExecutionAttempt(output, Collections.unmodifiableList(attempts));
  }

  public static ExecutionAttempt failure(List<Map<String, String>> attempts) {
    return new ExecutionAttempt(null, Collections.unmodifiableList(attempts));
  }
}
