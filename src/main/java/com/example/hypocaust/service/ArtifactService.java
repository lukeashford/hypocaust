package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for artifact persistence operations. Artifacts arrive already finalized (MANIFESTED or
 * FAILED) from executors. This service handles read operations and simple persistence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArtifactService {

  private final ArtifactRepository artifactRepository;
  private final ArtifactMapper artifactMapper;
  private final ArtifactIndexingService artifactIndexingService;

  /**
   * Get domain object for an artifact by ID.
   */
  public Optional<Artifact> getArtifact(UUID artifactId) {
    return artifactRepository.findById(artifactId).map(artifactMapper::toDomain);
  }

  /**
   * Get multiple artifacts by their IDs.
   */
  public List<Artifact> getArtifacts(Collection<UUID> artifactIds) {
    if (artifactIds == null || artifactIds.isEmpty()) {
      return List.of();
    }
    return artifactRepository.findAllById(artifactIds).stream()
        .map(artifactMapper::toDomain)
        .toList();
  }

  @Transactional
  public void delete(UUID artifactId, UUID projectId) {
    artifactRepository.findById(artifactId).ifPresent(entity -> {
      if (!entity.getProjectId().equals(projectId)) {
        throw new IllegalArgumentException(
            "Artifact " + artifactId + " does not belong to project " + projectId);
      }
      artifactRepository.delete(entity);
      log.info("Deleted artifact {} from project {}", artifactId, projectId);
    });
  }

  /**
   * Persist an artifact to the database. The artifact should already be finalized (MANIFESTED,
   * FAILED, or CANCELLED).
   */
  @Transactional
  Artifact persist(Artifact artifact, UUID projectId, UUID taskExecutionId) {
    ArtifactEntity saved = artifactRepository.save(artifactMapper.toEntity(
        artifact, projectId, taskExecutionId));
    Artifact persisted = artifactMapper.toDomain(saved);
    if (saved.getStatus() == ArtifactStatus.MANIFESTED) {
      artifactIndexingService.indexManifested(persisted, projectId);
    }
    return persisted;
  }
}
