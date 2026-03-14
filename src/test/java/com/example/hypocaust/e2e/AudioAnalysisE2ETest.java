package com.example.hypocaust.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.example.hypocaust.service.analysis.AudioAnalyzer;
import com.example.hypocaust.service.staging.PendingUpload;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AudioAnalysisE2ETest {

  // TODO: Fill in with a storage key of a short speech/dialog audio file in MinIO
  private static final String DIALOG_STORAGE_KEY = "TODO";
  // TODO: Fill in with a storage key of a non-dialog audio file (music/SFX) in MinIO
  private static final String MUSIC_STORAGE_KEY = "TODO";

  @Autowired
  private AudioAnalyzer audioAnalyzer;

  @Test
  void analyzeDialogAudio_transcribesAndClassifies() {
    PendingUpload upload = new PendingUpload(
        UUID.randomUUID(), DIALOG_STORAGE_KEY, null, "interview.mp3",
        "audio/mpeg", ArtifactKind.AUDIO, null, null, null, null);

    AnalysisResult result = audioAnalyzer.analyze(upload);

    assertThat(result.isFallback()).isFalse();
    assertThat(result.name()).matches("[a-z_]+");
    assertThat(result.title()).isNotBlank();
    assertThat(result.description()).isNotBlank();
    assertThat(result.enrichedMetadata()).isNotNull();
    assertThat(result.enrichedMetadata().path("audioType").asText()).isEqualTo("DIALOG");
    assertThat(result.enrichedMetadata().path("transcript").asText()).isNotBlank();
  }

  @Test
  void analyzeNonDialogAudio_classifiesAsNonDialog() {
    PendingUpload upload = new PendingUpload(
        UUID.randomUUID(), MUSIC_STORAGE_KEY, null, "background_music.mp3",
        "audio/mpeg", ArtifactKind.AUDIO, null, null, null, null);

    AnalysisResult result = audioAnalyzer.analyze(upload);

    assertThat(result.name()).isNotBlank();
    assertThat(result.title()).isNotBlank();
    assertThat(result.description()).isNotBlank();
    assertThat(result.enrichedMetadata()).isNotNull();
    assertThat(result.enrichedMetadata().path("audioType").asText()).isEqualTo("NON_DIALOG");
  }
}
