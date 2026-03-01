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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtifactServiceTest {

  private ArtifactRepository artifactRepository;
  private StorageService storageService;
  private ArtifactMapper artifactMapper;
  private ObjectMapper objectMapper;
  private ArtifactService artifactService;

  @BeforeEach
  void setUp() {
    artifactRepository = mock(ArtifactRepository.class);
    storageService = mock(StorageService.class);
    artifactMapper = mock(ArtifactMapper.class);
    objectMapper = new ObjectMapper();
    artifactService = new ArtifactService(artifactRepository, storageService, artifactMapper,
        objectMapper);
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
    when(artifactMapper.toDomain(entity)).thenReturn(Artifact.builder()
        .id(entity.getId())
        .name(entity.getName())
        .kind(entity.getKind())
        .status(entity.getStatus())
        .title("Title")
        .description("Desc")
        .build());

    Artifact result = artifactService.materialize(pendingArtifact, projectId, taskExecutionId);

    assertThat(result.id()).isEqualTo(entity.getId());
    verify(artifactRepository).save(entity);
  }

  @Test
  void shouldMaterializeTextArtifactInline() throws java.io.IOException {
    UUID projectId = UUID.randomUUID();
    UUID taskExecutionId = UUID.randomUUID();

    java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test", ".txt");
    java.nio.file.Files.writeString(tempFile, "Hello World");

    Artifact pendingArtifact = Artifact.builder()
        .name("text_artifact")
        .kind(ArtifactKind.TEXT)
        .status(ArtifactStatus.CREATED)
        .url(tempFile.toUri().toURL().toString())
        .title("Title")
        .description("Desc")
        .build();

    when(artifactMapper.toEntity(any(Artifact.class), eq(projectId),
        eq(taskExecutionId))).thenAnswer(invocation -> {
      Artifact arg = invocation.getArgument(0);
      return ArtifactEntity.builder()
          .name(arg.name())
          .kind(arg.kind())
          .status(arg.status())
          .inlineContent(arg.inlineContent())
          .projectId(projectId)
          .taskExecutionId(taskExecutionId)
          .build();
    });

    when(artifactRepository.save(any(ArtifactEntity.class))).thenAnswer(
        invocation -> {
          ArtifactEntity entity = invocation.getArgument(0);
          return entity;
        });

    when(artifactMapper.toDomain(any(ArtifactEntity.class))).thenAnswer(
        invocation -> {
          ArtifactEntity entity = invocation.getArgument(0);
          return Artifact.builder()
              .id(entity.getId())
              .name(entity.getName())
              .kind(entity.getKind())
              .status(entity.getStatus())
              .inlineContent(entity.getInlineContent())
              .title("Title")
              .description("Desc")
              .build();
        });

    Artifact result = artifactService.materialize(pendingArtifact, projectId, taskExecutionId);

    assertThat(result.id()).isNotNull();

    org.mockito.ArgumentCaptor<Artifact> artifactCaptor = org.mockito.ArgumentCaptor.forClass(
        Artifact.class);
    verify(artifactMapper).toEntity(artifactCaptor.capture(), eq(projectId), eq(taskExecutionId));

    Artifact materialized = artifactCaptor.getValue();
    assertThat(materialized.status()).isEqualTo(ArtifactStatus.MANIFESTED);
    assertThat(materialized.inlineContent()).isNotNull();
    assertThat(materialized.inlineContent().asText()).isEqualTo("Hello World");
    assertThat(materialized.url()).isNull(); // Should be null for inline

    java.nio.file.Files.deleteIfExists(tempFile);
  }

}
