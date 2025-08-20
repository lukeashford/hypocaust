package com.example.the_machine.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RunKind {
  FULL,
  PARTIAL;

  @JsonValue
  public String getValue() {
    return name();
  }
}