package com.example.hypocaust.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "SSE event type. Used as the `event` field in the SSE stream.", enumAsRef = true)
public enum EventType {
  // Artifact events (simplified)
  ARTIFACT_ADDED("artifact.added"),
  ARTIFACT_UPDATED("artifact.updated"),
  ARTIFACT_REMOVED("artifact.removed"),

  // TaskExecution events
  TASKEXECUTION_STARTED("taskexecution.started"),
  TASKEXECUTION_COMPLETED("taskexecution.completed"),
  TASKEXECUTION_FAILED("taskexecution.failed"),

  // Task progress events
  TASK_PROGRESS_UPDATED("task.progress.updated"),

  // Tool events
  TOOL_CALLING("tool.calling"),

  // Operator events (legacy, kept for backwards compatibility)
  OPERATOR_STARTED("operator.started"),
  OPERATOR_FINISHED("operator.finished"),
  OPERATOR_FAILED("operator.failed"),

  // Decomposer events
  DECOMPOSER_STARTED("decomposer.started"),
  DECOMPOSER_FINISHED("decomposer.finished"),
  DECOMPOSER_FAILED("decomposer.failed"),

  // Error events
  ERROR("error");

  private final String value;

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static EventType fromValue(String value) {
    for (final var type : EventType.values()) {
      if (type.value.equals(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown event type: " + value);
  }
}
