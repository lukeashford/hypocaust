package com.example.hypocaust.service.analysis;

import com.example.hypocaust.service.PdfTextExtractor;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.staging.PendingUpload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PdfAnalyzer implements ArtifactContentAnalyzer {

  private final TextAnalyzer textAnalyzer;
  private final StorageService storageService;
  private final PdfTextExtractor pdfTextExtractor;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    byte[] pdfBytes = storageService.fetch(upload.storageKey());
    String text = pdfTextExtractor.extract(pdfBytes);

    if (text.isBlank()) {
      return null;
    }

    return textAnalyzer.analyzeText(text);
  }
}
