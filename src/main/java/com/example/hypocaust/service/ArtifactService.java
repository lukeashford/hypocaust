package com.example.hypocaust.service;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact read operations. Write operations (create, edit, delete) are handled via
 * TaskExecutionContext hooks and materialized by VersionManagementService at TaskExecution
 * completion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;
  private final ArtifactMapper artifactMapper;

  /**
   * Get domain object for an artifact by ID
   */
  public Optional<Artifact> getArtifact(UUID artifactId) {
    return artifactRepository.findById(artifactId).map(artifactMapper::toDomain);
  }

  private Artifact downloadArtifact(Artifact pendingArtifact) {
    if (pendingArtifact.url() == null || pendingArtifact.url()
        .isBlank()) {
      throw new IllegalStateException("Artifact URL is blank.");
    }
    if (pendingArtifact.status() != ArtifactStatus.CREATED) {
      throw new IllegalStateException("Artifact is not in CREATED state.");
    }

    final int maxAttempts = 3;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      try (var stream = new URI(pendingArtifact.url()).toURL().openStream()) {
        byte[] data = stream.readAllBytes();
        String mimeType =
            pendingArtifact.kind() == ArtifactKind.IMAGE ? "image/png" : "application/octet-stream";

        log.info("Downloaded artifact {} with key {} (attempt {})", pendingArtifact.name(),
            storageService.store(data, mimeType), attempt);
        return pendingArtifact.withUrl(storageService.store(data, mimeType))
            .withStatus(ArtifactStatus.MANIFESTED);
      } catch (IOException e) {
        try {
          Thread.sleep(500L * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      } catch (URISyntaxException e) {
        log.warn("Invalid artifact URL: {}", pendingArtifact.url());
        return pendingArtifact.withStatus(ArtifactStatus.FAILED);
      }
    }

    return pendingArtifact.withStatus(ArtifactStatus.FAILED);
  }

  @Transactional
  public UUID materialize(Artifact pendingArtifact, UUID projectId, UUID taskExecutionId) {
    return artifactRepository.save(artifactMapper.toEntity(
        pendingArtifact.url() == null
            ? pendingArtifact
            : downloadArtifact(pendingArtifact),
        projectId, taskExecutionId)
    ).getId();
  }
}
