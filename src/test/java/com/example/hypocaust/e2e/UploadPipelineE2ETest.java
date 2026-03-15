package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.dto.UploadReceiptDto;
import com.example.hypocaust.service.ArtifactUploadService;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.staging.AnalyzedUpload;
import com.example.hypocaust.service.staging.StagingService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class UploadPipelineE2ETest {

  @Autowired
  private ArtifactUploadService uploadService;

  @Autowired
  private StagingService stagingService;

  @Autowired
  private ProjectService projectService;

  // TODO: Fill in with a real project ID from your MinIO-connected environment
  private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  @Test
  void uploadTextFile_analysisCompletes_batchConsumed() throws Exception {
    String content = "INT. OFFICE - DAY\n\nA DETECTIVE examines crime scene photos.";
    MockMultipartFile file = new MockMultipartFile("file", "script.txt", "text/plain",
        content.getBytes(StandardCharsets.UTF_8));

    UploadReceiptDto receipt = uploadService.upload(PROJECT_ID, file, null, null, null, null);

    assertThat(receipt.dataPackageId()).isNotNull();
    assertThat(receipt.batchId()).isNotNull();

    // Wait for analysis to complete (up to 30s)
    Thread.sleep(30_000);

    List<AnalyzedUpload> uploads = stagingService.consumeBatch(receipt.batchId());

    assertThat(uploads).hasSize(1);
    AnalyzedUpload upload = uploads.getFirst();
    AnalysisResult result = upload.analysisResult();
    assertThat(result).isNotNull();
    assertThat(result.name()).isNotBlank();
    assertThat(result.title()).isNotBlank();
    assertThat(result.description()).isNotBlank();
    assertThat(result.description()).isNotEqualTo("User-uploaded file (analysis unavailable)");
  }

  @Test
  void multipleUploads_sameBatch_allConsumed() throws Exception {
    MockMultipartFile file1 = new MockMultipartFile("file", "notes.txt", "text/plain",
        "Meeting notes from the project kickoff.".getBytes(StandardCharsets.UTF_8));
    MockMultipartFile file2 = new MockMultipartFile("file", "outline.txt", "text/plain",
        "Chapter 1: The beginning of the story.".getBytes(StandardCharsets.UTF_8));

    UploadReceiptDto receipt1 = uploadService.upload(PROJECT_ID, file1, null, null, null, null);
    UploadReceiptDto receipt2 = uploadService.upload(PROJECT_ID, file2, null, null, null,
        receipt1.batchId());

    assertThat(receipt1.batchId()).isEqualTo(receipt2.batchId());
    assertThat(receipt1.dataPackageId()).isNotEqualTo(receipt2.dataPackageId());

    Thread.sleep(30_000);

    List<AnalyzedUpload> uploads = stagingService.consumeBatch(receipt1.batchId());

    assertThat(uploads).hasSize(2);
    assertThat(uploads.stream().map(AnalyzedUpload::dataPackageId).distinct().count()).isEqualTo(2);
  }

  @Test
  void cancelUpload_removedFromBatch() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "temp.txt", "text/plain",
        "Temporary file to be cancelled.".getBytes(StandardCharsets.UTF_8));

    UploadReceiptDto receipt = uploadService.upload(PROJECT_ID, file, null, null, null, null);

    stagingService.cancelUpload(receipt.batchId(), receipt.dataPackageId());

    Thread.sleep(2_000);

    List<AnalyzedUpload> uploads = stagingService.consumeBatch(receipt.batchId());

    assertThat(uploads).isEmpty();
  }

  @Test
  void uploadWithClientMetadata_analysisStillRuns_clientValuesPreferred() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "image.txt", "text/plain",
        "This is actually a text file about images.".getBytes(StandardCharsets.UTF_8));

    UploadReceiptDto receipt = uploadService.upload(PROJECT_ID, file,
        "my_custom_name", "My Custom Title", "My custom description", null);

    Thread.sleep(30_000);

    List<AnalyzedUpload> uploads = stagingService.consumeBatch(receipt.batchId());

    assertThat(uploads).hasSize(1);
    AnalyzedUpload upload = uploads.getFirst();
    assertThat(upload.clientName()).isEqualTo("my_custom_name");
    assertThat(upload.clientTitle()).isEqualTo("My Custom Title");
    assertThat(upload.clientDescription()).isEqualTo("My custom description");
  }
}
