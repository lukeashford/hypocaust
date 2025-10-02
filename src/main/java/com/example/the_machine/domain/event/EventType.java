package com.example.the_machine.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {
  ARTIFACT_SCHEDULED("artifact.scheduled"),
  ARTIFACT_CREATED("artifact.created"),
  ARTIFACT_CANCELLED("artifact.cancelled"),
  ERROR("error"),
  RUN_SCHEDULED("run.scheduled"),
  RUN_STARTED("run.started"),
  RUN_COMPLETED("run.completed"),
  TOOL_CALLING("tool.calling");

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