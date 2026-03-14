package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.analysis.VideoAnalyzer;
import com.example.hypocaust.service.staging.PendingUpload;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VideoAnalysisE2ETest {

  // TODO: Fill in with a storage key of a video file uploaded to MinIO
  private static final String STORAGE_KEY = "TODO";

  @Autowired
  private VideoAnalyzer videoAnalyzer;

  @Test
  void analyzeVideo_returnsFallbackUntilFfmpegIntegrated() {
    PendingUpload upload = new PendingUpload(
        UUID.randomUUID(), STORAGE_KEY, null, "scene.mp4",
        "video/mp4", ArtifactKind.VIDEO, null, null, null, null);

    AnalysisResult result = videoAnalyzer.analyze(upload);

    // Until FFmpeg is integrated, this should return the duration fallback
    assertThat(result.name()).isEqualTo("uploaded_video");
    assertThat(result.title()).isEqualTo("Uploaded Video");
    assertThat(result.description()).contains("User-uploaded video");
  }
}
