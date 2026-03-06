package com.example.hypocaust.service;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.dto.ArtifactDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Converts internal {@link Artifact} objects (storageKey-based) to client-facing
 * {@link ArtifactDto} objects (presigned URL-based). URL generation always happens on demand,
 * never stored.
 */
@Service
@RequiredArgsConstructor
public class ArtifactExternalizer {

  private static final int PRESIGNED_URL_EXPIRY_SECONDS = 600;

  private final StorageService storageService;

  public ArtifactDto externalize(Artifact artifact) {
    String url = null;
    if (artifact.storageKey() != null && !artifact.storageKey().isBlank()) {
      url = storageService.generatePresignedUrl(artifact.storageKey(), PRESIGNED_URL_EXPIRY_SECONDS);
    }
    return new ArtifactDto(
        artifact.id(),
        artifact.name(),
        artifact.kind(),
        url,
        artifact.inlineContent(),
        artifact.title(),
        artifact.description(),
        artifact.status(),
        artifact.metadata(),
        artifact.mimeType(),
        artifact.errorMessage()
    );
  }
}
