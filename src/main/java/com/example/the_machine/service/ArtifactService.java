package com.example.the_machine.service;

import com.example.the_machine.db.ArtifactEntity;
import com.example.the_machine.db.ArtifactEntity.Kind;
import com.example.the_machine.db.ArtifactEntity.Status;
import com.example.the_machine.domain.event.ArtifactCreatedEvent;
import com.example.the_machine.domain.event.ArtifactScheduledEvent;
import com.example.the_machine.dto.ArtifactMetadataDto;
import com.example.the_machine.exception.ArtifactNotFoundException;
import com.example.the_machine.exception.ArtifactNotReadyException;
import com.example.the_machine.mapper.ArtifactMapper;
import com.example.the_machine.operator.RunContextHolder;
import com.example.the_machine.repo.ArtifactRepository;
import com.example.the_machine.service.events.EventService;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final StorageService storageService;
  private final EventService eventService;
  private final ArtifactMapper artifactMapper;

  /**
   * Get all artifacts for a project
   */
  public List<ArtifactEntity> getProjectArtifacts(UUID projectId) {
    return artifactRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
  }

  /**
   * Get metadata DTOs for all artifacts in a project
   */
  public List<ArtifactMetadataDto> getProjectArtifactMetadata(UUID projectId) {
    return getProjectArtifacts(projectId).stream()
        .map(artifactMapper::toMetadataDto)
        .toList();
  }

  /**
   * Get a specific artifact by ID
   */
  public ArtifactEntity getArtifact(UUID artifactId) {
    return artifactRepository.findById(artifactId)
        .orElseThrow(() -> new ArtifactNotFoundException(
            String.format("Artifact %s not found", artifactId)));
  }

  /**
   * Get metadata DTO for an artifact by ID
   */
  public ArtifactMetadataDto getArtifactMetadata(UUID artifactId) {
    return artifactMapper.toMetadataDto(getArtifact(artifactId));
  }

  /**
   * Download file-based artifact from storage
   */
  public InputStream downloadArtifact(UUID artifactId) {
    final var artifact = getArtifact(artifactId);

    // Validate artifact is file-based and has storage key
    if (artifact.getStorageKey() == null) {
      throw new IllegalStateException(
          String.format("Artifact %s does not have a storage key", artifactId));
    }

    if (artifact.getStatus() != Status.CREATED) {
      throw new ArtifactNotReadyException(
          String.format("Artifact %s is not ready (status: %s)",
              artifactId, artifact.getStatus()));
    }

    log.info("Downloading artifact {} from storage key: {}",
        artifactId, artifact.getStorageKey());

    return storageService.retrieve(artifact.getStorageKey());
  }

  @Transactional
  public UUID schedule(
      Kind kind,
      String title,
      String mime
  ) {
    final var projectId = RunContextHolder.getProjectId();
    final var runId = RunContextHolder.getRunId();
    log.debug("Scheduling artifact for project {}: {}", projectId, title);

    final var artifact = new ArtifactEntity(
        projectId,
        runId,
        kind,
        Status.SCHEDULED,
        title,
        mime,
        null,
        null,
        null,
        null
    );
    artifactRepository.save(artifact);
    eventService.publish(new ArtifactScheduledEvent(projectId, artifact.getId()));

    return artifact.getId();
  }

  @Transactional
  public void updateArtifact(UUID artifactId, JsonNode content, JsonNode metadata) {
    log.debug("Updating artifact {}.\nContent: {} \nMetadata: {}", artifactId, content, metadata);
    final var artifact = getArtifact(artifactId);
    artifact.setContent(content);
    artifact.setMetadata(metadata);
    artifact.setStatus(Status.CREATED);
    artifactRepository.save(artifact);

    eventService.publish(new ArtifactCreatedEvent(artifact.getProjectId(), artifact.getId()));
  }
}