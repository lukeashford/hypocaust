package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.analysis.PdfAnalyzer;
import com.example.hypocaust.service.staging.PendingUpload;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PdfAnalysisE2ETest {

  // TODO: Fill in with a storage key of a PDF file uploaded to MinIO
  private static final String STORAGE_KEY = "TODO";

  @Autowired
  private PdfAnalyzer pdfAnalyzer;

  @Test
  void analyzePdf_extractsTextAndAnalyzes() {
    PendingUpload upload = new PendingUpload(
        UUID.randomUUID(), STORAGE_KEY, null, "report.pdf",
        "application/pdf", ArtifactKind.PDF, null, null, null, null);

    AnalysisResult result = pdfAnalyzer.analyze(upload);

    assertThat(result).isNotNull();
    assertThat(result.name()).matches("[a-z_]+");
    assertThat(result.title()).isNotBlank();
    assertThat(result.description()).isNotBlank();
  }
}
