package com.example.hypocaust.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a task execution", enumAsRef = true)
public enum RunStatus {
  QUEUED,
  RUNNING,
  REQUIRES_ACTION,
  COMPLETED,
  PARTIALLY_SUCCESSFUL,
  FAILED,
  CANCELLED;

  @JsonValue
  public String getValue() {
    return name();
  }
}
