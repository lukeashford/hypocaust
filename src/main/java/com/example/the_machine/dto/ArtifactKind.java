package com.example.the_machine.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ArtifactKind {
  STRUCTURED_JSON,
  IMAGE,
  PDF,
  AUDIO,
  VIDEO;

  @JsonValue
  public String getValue() {
    return name();
  }
}