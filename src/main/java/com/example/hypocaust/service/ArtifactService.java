package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.exception.ArtifactNotReadyException;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact read operations. Write operations (create, edit, delete) are handled via
 * TaskExecutionContext hooks and materialized by ArtifactVersionManagementService at TaskExecution
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
  private final ArtifactVersionManagementService versionService;

  /**
   * Get all artifacts for a project (across all TaskExecutions)
   */
  public List<ArtifactEntity> getProjectArtifacts(UUID projectId) {
    return artifactRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
  }

  /**
   * Get domain objects for all artifacts in a project
   */
  public List<Artifact> getProjectArtifactsAsDomain(UUID projectId) {
    return getProjectArtifacts(projectId).stream()
        .map(artifactMapper::toDomain)
        .toList();
  }

  /**
   * Get artifacts at a specific TaskExecution (point-in-time view). Returns the artifacts that were
   * visible at that TaskExecution.
   */
  public List<ArtifactEntity> getArtifactsAtTaskExecution(UUID taskExecutionId) {
    return versionService.getArtifactsAtTaskExecution(taskExecutionId);
  }

  /**
   * Get domain objects for artifacts at a specific TaskExecution.
   */
  public List<Artifact> getArtifactsAtTaskExecutionAsDomain(UUID taskExecutionId) {
    return getArtifactsAtTaskExecution(taskExecutionId).stream()
        .map(artifactMapper::toDomain)
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
   * Get domain object for an artifact by ID
   */
  public Artifact getArtifactDomain(UUID artifactId) {
    return artifactMapper.toDomain(getArtifact(artifactId));
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

    if (artifact.getStatus() != ArtifactStatus.CREATED) {
      throw new ArtifactNotReadyException(
          String.format("Artifact %s is not ready (status: %s)",
              artifactId, artifact.getStatus()));
    }

    log.info("Downloading artifact {} from storage key: {}",
        artifactId, artifact.getStorageKey());

    return storageService.retrieve(artifact.getStorageKey());
  }
}
