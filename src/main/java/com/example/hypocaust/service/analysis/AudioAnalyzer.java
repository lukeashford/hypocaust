package com.example.hypocaust.service.analysis;

import com.example.hypocaust.service.TranscriptionService;
import com.example.hypocaust.service.analysis.TextComprehensionService.ContentDescription;
import com.example.hypocaust.service.staging.PendingUpload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AudioAnalyzer implements ArtifactContentAnalyzer {

  private static final int MIN_CONFIDENT_WORDS = 4;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TranscriptionService transcriptionService;
  private final TextComprehensionService comprehensionService;

  @Override
  public AnalysisResult analyze(PendingUpload upload) {
    try {
      String transcript = transcriptionService.transcribeFull(upload.storageKey());

      if (isConfident(transcript)) {
        return analyzeAsDialog(transcript);
      }

      return analyzeAsNonDialog(upload);
    } catch (Exception e) {
      log.debug("Audio analysis failed for upload {}: {}", upload.dataPackageId(), e.getMessage());
      return buildFallback();
    }
  }

  private AnalysisResult analyzeAsDialog(String transcript) {
    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.put("audioType", "DIALOG");
    metadata.put("transcript", transcript);

    ContentDescription description = comprehensionService.analyze(transcript);
    if (description == null) {
      return buildFallback();
    }

    return new AnalysisResult(description.name(), description.title(),
        description.description(), metadata);
  }

  private AnalysisResult analyzeAsNonDialog(PendingUpload upload) {
    ObjectNode metadata = MAPPER.createObjectNode();
    metadata.put("audioType", "NON_DIALOG");

    String baseName = sanitizeFilename(upload.originalFilename());
    String name = baseName != null ? baseName : "uploaded_audio";
    String title = upload.originalFilename() != null
        ? upload.originalFilename() : "Uploaded Audio";

    return new AnalysisResult(name, title, "Non-dialog audio file", metadata);
  }

  private AnalysisResult buildFallback() {
    return new AnalysisResult("uploaded_audio", "Uploaded Audio",
        "User-uploaded audio file", null);
  }

  private boolean isConfident(String transcript) {
    return transcript != null && !transcript.isBlank()
        && transcript.split("\\s+").length >= MIN_CONFIDENT_WORDS;
  }

  private static String sanitizeFilename(String filename) {
    if (filename == null || filename.isBlank()) {
      return null;
    }
    int dotIndex = filename.lastIndexOf('.');
    String base = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    return base.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
  }
}
