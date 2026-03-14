package com.example.hypocaust.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;

public record AnalysisResult(String name, String title, String description,
    boolean hasIndexableContent, String indexableText, JsonNode enrichedMetadata) {

  public static final AnalysisResult FALLBACK = new AnalysisResult(
      null, null, null, false, null, null);

  public boolean isFallback() {
    return this == FALLBACK || name == null;
  }
}
