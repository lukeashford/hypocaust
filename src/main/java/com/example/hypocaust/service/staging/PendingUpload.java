package com.example.hypocaust.service.staging;

import com.example.hypocaust.domain.ArtifactKind;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import java.util.concurrent.Future;

public record PendingUpload(
    UUID dataPackageId,
    String storageKey,
    JsonNode inlineContent,
    String originalFilename,
    String mimeType,
    ArtifactKind kind,
    String clientName,
    String clientTitle,
    String clientDescription,
    Future<?> analysisFuture
) {

  PendingUpload withFuture(Future<?> future) {
    return new PendingUpload(dataPackageId, storageKey, inlineContent, originalFilename,
        mimeType, kind, clientName, clientTitle, clientDescription, future);
  }
}
