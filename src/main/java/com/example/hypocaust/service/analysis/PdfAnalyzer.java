package com.example.hypocaust.service.analysis;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.service.PdfTextExtractor;
import com.example.hypocaust.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PdfAnalyzer implements ArtifactContentAnalyzer {

  private final TextAnalyzer textAnalyzer;
  private final StorageService storageService;
  private final PdfTextExtractor pdfTextExtractor;

  @Override
  public AnalysisResult analyze(ArtifactEntity entity) {
    byte[] pdfBytes = storageService.fetch(entity.getStorageKey());
    String text = pdfTextExtractor.extract(pdfBytes);

    if (text.isBlank()) {
      return AnalysisResult.FALLBACK;
    }

    return textAnalyzer.analyzeText(text);
  }
}
