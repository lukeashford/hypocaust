package com.example.hypocaust.service.analysis;

import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.analysis.TextComprehensionService.ContentDescription;
import com.example.hypocaust.service.staging.PendingUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextAnalyzer implements ArtifactContentAnalyzer {

  private final TextComprehensionService comprehensionService;
  private final StorageService storageService;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    String text = extractText(upload);
    return analyzeText(text);
  }

  AnalysisResult analyzeText(String text) {
    ContentDescription description = comprehensionService.analyze(text);
    if (description == null) {
      return null;
    }
    return new AnalysisResult(description.name(), description.title(),
        description.description(), null);
  }

  private String extractText(PendingUpload upload) {
    if (upload.inlineContent() != null) {
      return upload.inlineContent().isTextual()
          ? upload.inlineContent().asText()
          : upload.inlineContent().toString();
    }
    if (upload.storageKey() != null) {
      byte[] bytes = storageService.fetch(upload.storageKey());
      return new String(bytes);
    }
    return "";
  }
}
