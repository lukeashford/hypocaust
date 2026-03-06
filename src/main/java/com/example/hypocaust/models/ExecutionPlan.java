package com.example.hypocaust.models;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/**
 * The result of the planning step: provider-specific input, a mapping from provider output keys to
 * artifact names, and an optional error message.
 *
 * <p>The {@code outputMapping} bridges the gap between what the model produces and which artifact
 * each output should land in. Keys are provider-side output identifiers (chosen by the planner and
 * matched by {@link AbstractModelExecutor#extractOutputs}); values are artifact names from the
 * gestating list. Extra outputs that have no mapping entry are silently ignored.
 *
 * @param providerInput the provider-specific JSON input to send to the model API
 * @param outputMapping outputKey → artifact name; every gestating artifact must appear as a value
 * @param errorMessage non-null if the planner determined the task cannot be fulfilled
 */
public record ExecutionPlan(
    JsonNode providerInput,
    Map<String, String> outputMapping,
    String errorMessage
) {

  public boolean hasError() {
    return errorMessage != null;
  }

  public static ExecutionPlan error(String message) {
    return new ExecutionPlan(null, Map.of(), message);
  }
}
