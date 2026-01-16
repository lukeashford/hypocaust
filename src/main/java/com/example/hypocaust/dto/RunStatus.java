package com.example.hypocaust.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RunStatus {
  QUEUED,
  RUNNING,
  REQUIRES_ACTION,
  COMPLETED,
  FAILED,
  CANCELLED;

  @JsonValue
  public String getValue() {
    return name();
  }
}