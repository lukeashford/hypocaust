package com.example.hypocaust.service.analysis;

public record AnalysisResult(String name, String title, String description,
    boolean hasIndexableContent) {

  static final AnalysisResult FALLBACK = new AnalysisResult(
      null, "Unknown Upload", "User-uploaded file (analysis failed)", false);

  boolean isFallback() {
    return this == FALLBACK || name == null;
  }
}
