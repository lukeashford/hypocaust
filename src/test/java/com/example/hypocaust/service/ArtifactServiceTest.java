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
  private StorageService storageService;
  private ArtifactMapper artifactMapper;
  private ArtifactService artifactService;

  @BeforeEach
  void setUp() {
    artifactRepository = mock(ArtifactRepository.class);
    storageService = mock(StorageService.class);
    artifactMapper = mock(ArtifactMapper.class);
    artifactService = new ArtifactService(artifactRepository, storageService, artifactMapper);
  }

  @Test
  void shouldGetArtifactWithExternalizedUrl() {
    ArtifactEntity entity = ArtifactEntity.builder()
        .name("test")
        .kind(ArtifactKind.IMAGE)
        .storageKey("blobs/12/34/hash.png")
        .status(ArtifactStatus.MANIFESTED)
        .build();
    UUID artifactId = entity.getId();

    Artifact domainArtifact = Artifact.builder()
        .id(artifactId)
        .name("test")
        .kind(ArtifactKind.IMAGE)
        .url("http://presigned-url")
        .status(ArtifactStatus.MANIFESTED)
        .title("Test Title")
        .description("Test Description")
        .build();

    when(artifactRepository.findById(artifactId)).thenReturn(Optional.of(entity));
    when(artifactMapper.toDomain(entity)).thenReturn(domainArtifact);

    Optional<Artifact> result = artifactService.getArtifact(artifactId);

    assertThat(result).isPresent();
    assertThat(result.get().url()).isEqualTo("http://presigned-url");
    verify(artifactMapper).toDomain(entity);
  }

  @Test
  void shouldMaterializeArtifact() {
    UUID projectId = UUID.randomUUID();
    UUID taskExecutionId = UUID.randomUUID();
    Artifact pendingArtifact = Artifact.builder()
        .name("new_artifact")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.CREATED)
        .url("http://temp-url")
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

    when(storageService.store(any(byte[].class), any(String.class))).thenReturn(
        "blobs/ab/cd/hash.png");
    when(artifactMapper.toEntity(any(Artifact.class), eq(projectId),
        eq(taskExecutionId))).thenReturn(entity);
    when(artifactRepository.save(entity)).thenReturn(entity);

    UUID resultId = artifactService.materialize(pendingArtifact, projectId, taskExecutionId);

    assertThat(resultId).isEqualTo(entity.getId());
    verify(artifactRepository).save(entity);
  }
}
