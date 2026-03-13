package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.repo.ArtifactRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtifactServiceTest {

  private ArtifactRepository artifactRepository;
  private ArtifactMapper artifactMapper;
  private ArtifactIndexingService artifactIndexingService;
  private ArtifactService artifactService;

  @BeforeEach
  void setUp() {
    artifactRepository = mock(ArtifactRepository.class);
    artifactMapper = mock(ArtifactMapper.class);
    artifactIndexingService = mock(ArtifactIndexingService.class);
    artifactService = new ArtifactService(artifactRepository, artifactMapper,
        artifactIndexingService);
  }

  @Test
  void shouldGetArtifact() {
    ArtifactEntity entity = ArtifactEntity.builder()
        .name("test")
        .kind(ArtifactKind.IMAGE)
        .storageKey("blobs/12/34/hash.png")
        .status(ArtifactStatus.MANIFESTED)
        .build();
    UUID artifactId = entity.getId();

    Artifact domainArtifact = Artifact.builder()
        .name("test")
        .kind(ArtifactKind.IMAGE)
        .storageKey("blobs/12/34/hash.png")
        .status(ArtifactStatus.MANIFESTED)
        .title("Test Title")
        .description("Test Description")
        .build();

    when(artifactRepository.findById(artifactId)).thenReturn(Optional.of(entity));
    when(artifactMapper.toDomain(entity)).thenReturn(domainArtifact);

    Optional<Artifact> result = artifactService.getArtifact(artifactId);

    assertThat(result).isPresent();
    assertThat(result.get().storageKey()).isEqualTo("blobs/12/34/hash.png");
    verify(artifactMapper).toDomain(entity);
  }

  @Test
  void shouldPersistArtifact() {
    UUID projectId = UUID.randomUUID();
    UUID taskExecutionId = UUID.randomUUID();
    UUID artifactId = UUID.randomUUID();

    Artifact artifact = Artifact.builder()
        .name("new_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/hash.png")
        .mimeType("image/png")
        .title("Title")
        .description("Desc")
        .build();

    ArtifactEntity entity = ArtifactEntity.builder()
        .name("new_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/hash.png")
        .projectId(projectId)
        .taskExecutionId(taskExecutionId)
        .build();

    Artifact persisted = Artifact.builder()
        .id(artifactId)
        .name("new_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/hash.png")
        .title("Title")
        .description("Desc")
        .build();

    when(artifactMapper.toEntity(artifact, projectId, taskExecutionId)).thenReturn(entity);
    when(artifactRepository.save(entity)).thenReturn(entity);
    when(artifactMapper.toDomain(entity)).thenReturn(persisted);

    Artifact result = artifactService.persist(artifact, projectId, taskExecutionId);

    assertThat(result.id()).isEqualTo(artifactId);
    verify(artifactRepository).save(entity);
    verify(artifactMapper).toEntity(artifact, projectId, taskExecutionId);
  }

  @Test
  void shouldPersistFailedArtifact() {
    UUID projectId = UUID.randomUUID();
    UUID taskExecutionId = UUID.randomUUID();
    UUID artifactId = UUID.randomUUID();

    Artifact artifact = Artifact.builder()
        .name("failed_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.FAILED)
        .errorMessage("Download failed after 3 attempts")
        .title("Title")
        .description("Desc")
        .build();

    ArtifactEntity entity = ArtifactEntity.builder()
        .name("failed_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.FAILED)
        .errorMessage("Download failed after 3 attempts")
        .projectId(projectId)
        .taskExecutionId(taskExecutionId)
        .build();

    Artifact persisted = Artifact.builder()
        .id(artifactId)
        .name("failed_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.FAILED)
        .errorMessage("Download failed after 3 attempts")
        .title("Title")
        .description("Desc")
        .build();

    when(artifactMapper.toEntity(artifact, projectId, taskExecutionId)).thenReturn(entity);
    when(artifactRepository.save(entity)).thenReturn(entity);
    when(artifactMapper.toDomain(entity)).thenReturn(persisted);

    Artifact result = artifactService.persist(artifact, projectId, taskExecutionId);

    assertThat(result.id()).isEqualTo(artifactId);
    assertThat(result.status()).isEqualTo(ArtifactStatus.FAILED);
    assertThat(result.errorMessage()).isEqualTo("Download failed after 3 attempts");
  }
}
