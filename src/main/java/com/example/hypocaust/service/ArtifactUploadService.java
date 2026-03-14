package com.example.hypocaust.service;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.dto.UploadReceiptDto;
import com.example.hypocaust.service.staging.PendingUpload;
import com.example.hypocaust.service.staging.StagingBatch;
import com.example.hypocaust.service.staging.StagingService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactUploadService {

  private final StorageService storageService;
  private final StagingService stagingService;
  private final ArtifactAnalysisService artifactAnalysisService;

  public UploadReceiptDto upload(UUID projectId, MultipartFile file, String name, String title,
      String description, UUID batchId) {
    String mimeType = file.getContentType();
    String storageKey;
    try {
      storageKey = storageService.store(file.getInputStream(), file.getSize(), mimeType);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read uploaded file", e);
    }

    StagingBatch batch = stagingService.getOrCreateBatch(batchId);
    UUID dataPackageId = UUID.randomUUID();

    PendingUpload upload = new PendingUpload(
        dataPackageId,
        storageKey,
        null,
        file.getOriginalFilename(),
        mimeType,
        kindFromMimeType(mimeType),
        nonBlank(name),
        nonBlank(title),
        nonBlank(description),
        null
    );

    artifactAnalysisService.analyzeAsync(upload, batch);

    log.debug("Staged upload: dataPackageId={}, batchId={}, project={}",
        dataPackageId, batch.getBatchId(), projectId);

    return new UploadReceiptDto(dataPackageId, batch.getBatchId());
  }

  private static ArtifactKind kindFromMimeType(String mimeType) {
    if (mimeType == null) {
      return ArtifactKind.OTHER;
    }
    if (mimeType.startsWith("image/")) {
      return ArtifactKind.IMAGE;
    }
    if (mimeType.startsWith("audio/")) {
      return ArtifactKind.AUDIO;
    }
    if (mimeType.startsWith("video/")) {
      return ArtifactKind.VIDEO;
    }
    if (mimeType.startsWith("text/")) {
      return ArtifactKind.TEXT;
    }
    if (mimeType.equals("application/pdf")) {
      return ArtifactKind.PDF;
    }
    return ArtifactKind.OTHER;
  }

  private static String nonBlank(String value) {
    return (value != null && !value.isBlank()) ? value : null;
  }
}
