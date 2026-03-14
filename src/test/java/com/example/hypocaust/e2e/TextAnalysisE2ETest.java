package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.service.StorageService;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.analysis.TextAnalyzer;
import com.example.hypocaust.service.staging.PendingUpload;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TextAnalysisE2ETest {

  // TODO: Fill in with a storage key of a text file uploaded to MinIO
  private static final String STORAGE_KEY = "TODO";

  @Autowired
  private TextAnalyzer textAnalyzer;

  @Autowired
  private StorageService storageService;

  @Test
  void analyzeTextFile_returnsReasonableResult() {
    PendingUpload upload = new PendingUpload(
        UUID.randomUUID(), STORAGE_KEY, null, "sample_script.txt",
        "text/plain", ArtifactKind.TEXT, null, null, null, null);

    AnalysisResult result = textAnalyzer.analyze(upload);

    assertThat(result.isFallback()).isFalse();
    assertThat(result.name()).matches("[a-z_]+");
    assertThat(result.title()).isNotBlank();
    assertThat(result.description()).isNotBlank();
  }

  @Test
  void analyzeTextFromInlineContent_returnsReasonableResult() {
    String sampleText = "INT. SPACESHIP BRIDGE - NIGHT\n\n"
        + "CAPTAIN JONES stares at the viewscreen. Stars blur past.\n\n"
        + "CAPTAIN JONES\nWe're running out of time.";

    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    var textNode = mapper.getNodeFactory().textNode(sampleText);

    PendingUpload upload = new PendingUpload(
        UUID.randomUUID(), null, textNode, "dialogue.txt",
        "text/plain", ArtifactKind.TEXT, null, null, null, null);

    AnalysisResult result = textAnalyzer.analyze(upload);

    assertThat(result.isFallback()).isFalse();
    assertThat(result.name()).matches("[a-z_]+");
    assertThat(result.title()).isNotBlank();
    assertThat(result.description()).isNotBlank();
  }
}
