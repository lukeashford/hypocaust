package com.example.hypocaust.integration;

import com.fasterxml.jackson.databind.JsonNode;

public record ExecutionPlan(
    JsonNode providerInput,
    String errorMessage
) {

  public boolean hasError() {
    return errorMessage != null;
  }

  public static ExecutionPlan error(String message) {
    return new ExecutionPlan(null, message);
  }
}
