package com.example.the_machine.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EventType {
  RUN_CREATED("run.created"),
  RUN_UPDATED("run.updated"),
  MESSAGE_DELTA("message.delta"),
  MESSAGE_COMPLETED("message.completed"),
  ARTIFACT_CREATED("artifact.created"),
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