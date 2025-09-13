package com.example.the_machine.service;

import com.example.the_machine.common.IdGenerator;
import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.repo.ArtifactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling artifact creation and management using direct type-safe methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final IdGenerator idGenerator;

  // Direct artifact creation methods

  /**
   * Creates an artifact directly with metadata - no JSON parsing needed.
   *
   * @param threadId the thread ID
   * @param runId the run ID
   * @param kind the artifact kind
   * @param stage the workflow stage
   * @param title the artifact title
   * @param mime the MIME type (optional)
   * @return the created artifact entity
   */
  @Transactional
  public ArtifactEntity createArtifact(
      UUID threadId,
      UUID runId,
      ArtifactEntity.Kind kind,
      ArtifactEntity.Stage stage,
      String title,
      String mime
  ) {
    log.debug("Creating artifact: kind={}, stage={}, title={}", kind, stage, title);

    final var artifact = ArtifactEntity.builder()
        .id(idGenerator.newId())
        .threadId(threadId)
        .runId(runId)
        .kind(kind)
        .stage(stage)
        .status(ArtifactEntity.Status.PENDING)
        .title(title)
        .mime(mime)
        .createdAt(Instant.now())
        .build();

    final var saved = artifactRepository.save(artifact);
    log.info("Artifact created: {} for thread: {}", saved.getId(), threadId);
    return saved;
  }

  /**
   * Updates artifact with structured content (JSON).
   *
   * @param artifactId the artifact ID
   * @param content the content to set
   */
  @Transactional
  public void setContent(UUID artifactId, JsonNode content) {
    final var artifact = artifactRepository.findById(artifactId)
        .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));

    artifact.setContent(content);
    artifact.setStatus(ArtifactEntity.Status.DONE);
    artifactRepository.save(artifact);

    log.debug("Content set for artifact: {}", artifactId);
  }

  /**
   * Updates artifact with file storage information.
   *
   * @param artifactId the artifact ID
   * @param storageKey the storage location
   * @param mime the MIME type
   */
  @Transactional
  public void setStorageKey(UUID artifactId, String storageKey, String mime) {
    final var artifact = artifactRepository.findById(artifactId)
        .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));

    artifact.setStorageKey(storageKey);
    artifact.setMime(mime);
    artifact.setStatus(ArtifactEntity.Status.DONE);
    artifactRepository.save(artifact);

    log.debug("Storage key set for artifact: {} -> {}", artifactId, storageKey);
  }

  /**
   * Updates artifact metadata.
   *
   * @param artifactId the artifact ID
   * @param metadata the metadata to set
   */
  @Transactional
  public void setMetadata(UUID artifactId, JsonNode metadata) {
    final var artifact = artifactRepository.findById(artifactId)
        .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));

    artifact.setMetadata(metadata);
    artifactRepository.save(artifact);

    log.debug("Metadata set for artifact: {}", artifactId);
  }

}