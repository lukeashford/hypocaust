package com.example.hypocaust.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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

  // Operator events
  OPERATOR_STARTED("operator.started"),
  OPERATOR_FINISHED("operator.finished"),
  OPERATOR_FAILED("operator.failed"),

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
