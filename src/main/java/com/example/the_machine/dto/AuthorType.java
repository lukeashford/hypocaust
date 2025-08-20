package com.example.the_machine.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum AuthorType {
  USER,
  ASSISTANT,
  TOOL,
  SYSTEM;

  @JsonValue
  public String getValue() {
    return name();
  }
}