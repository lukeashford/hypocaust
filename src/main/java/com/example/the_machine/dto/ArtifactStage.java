package com.example.the_machine.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ArtifactStage {
  PLAN,
  ANALYSIS,
  SCRIPT,
  IMAGES,
  DECK;

  @JsonValue
  public String getValue() {
    return name();
  }
}