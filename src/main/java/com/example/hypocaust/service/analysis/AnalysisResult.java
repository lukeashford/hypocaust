package com.example.hypocaust.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;

public record AnalysisResult(String name, String title, String description,
    JsonNode enrichedMetadata) {

  public static final AnalysisResult FALLBACK = new AnalysisResult(
      null, null, null, null);

  public boolean isFallback() {
    return this == FALLBACK || name == null;
  }
}
