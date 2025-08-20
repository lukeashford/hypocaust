package com.example.the_machine.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ArtifactStatus {
  PENDING,
  RUNNING,
  DONE,
  FAILED;

  @JsonValue
  public String getValue() {
    return name();
  }
}