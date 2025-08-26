package com.example.the_machine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.the_machine.common.IdGenerator;
import com.example.the_machine.domain.ArtifactEntity;
import com.example.the_machine.repo.ArtifactRepository;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtifactServiceTest {

  @Mock
  private ArtifactRepository artifactRepository;

  @Mock
  private IdGenerator idGenerator;

  @InjectMocks
  private ArtifactService artifactService;

  @Captor
  private ArgumentCaptor<ArtifactEntity> artifactCaptor;

  @Test
  void shouldCreateStructuredJsonArtifact() {
    // Given
    val threadId = UUID.randomUUID();
    val runId = UUID.randomUUID();
    val artifactId = UUID.randomUUID();

    when(idGenerator.newId()).thenReturn(artifactId);
    when(artifactRepository.save(any(ArtifactEntity.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    // When
    val result = artifactService.createArtifact(
        threadId, runId,
        ArtifactEntity.Kind.STRUCTURED_JSON,
        ArtifactEntity.Stage.SCRIPT,
        "Apple Marketing Pitch",
        null
    );

    // Then
    verify(artifactRepository).save(artifactCaptor.capture());
    val savedArtifact = artifactCaptor.getValue();

    assertThat(savedArtifact.getId()).isEqualTo(artifactId);
    assertThat(savedArtifact.getThreadId()).isEqualTo(threadId);
    assertThat(savedArtifact.getRunId()).isEqualTo(runId);
    assertThat(savedArtifact.getKind()).isEqualTo(ArtifactEntity.Kind.STRUCTURED_JSON);
    assertThat(savedArtifact.getStage()).isEqualTo(ArtifactEntity.Stage.SCRIPT);
    assertThat(savedArtifact.getStatus()).isEqualTo(ArtifactEntity.Status.PENDING);
    assertThat(savedArtifact.getTitle()).isEqualTo("Apple Marketing Pitch");
    assertThat(savedArtifact.getContent()).isNull(); // No content initially
    assertThat(savedArtifact.getCreatedAt()).isNotNull();
    assertThat(result).isEqualTo(savedArtifact);
  }

  @Test
  void shouldCreateImageArtifact() {
    // Given
    val threadId = UUID.randomUUID();
    val runId = UUID.randomUUID();
    val artifactId = UUID.randomUUID();

    when(idGenerator.newId()).thenReturn(artifactId);
    when(artifactRepository.save(any(ArtifactEntity.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    // When
    val result = artifactService.createArtifact(
        threadId, runId,
        ArtifactEntity.Kind.IMAGE,
        ArtifactEntity.Stage.IMAGES,
        "Marketing Visual 1",
        "image/png"
    );

    // Then
    verify(artifactRepository).save(artifactCaptor.capture());
    val savedArtifact = artifactCaptor.getValue();

    assertThat(savedArtifact.getKind()).isEqualTo(ArtifactEntity.Kind.IMAGE);
    assertThat(savedArtifact.getStage()).isEqualTo(ArtifactEntity.Stage.IMAGES);
    assertThat(savedArtifact.getTitle()).isEqualTo("Marketing Visual 1");
    assertThat(savedArtifact.getMime()).isEqualTo("image/png");
    assertThat(savedArtifact.getStorageKey()).isNull(); // No storage key initially
    assertThat(savedArtifact.getContent()).isNull(); // Images don't have inline content
    assertThat(result).isEqualTo(savedArtifact);
  }

  @Test
  void shouldCreatePdfArtifact() {
    // Given
    val threadId = UUID.randomUUID();
    val runId = UUID.randomUUID();
    val artifactId = UUID.randomUUID();

    when(idGenerator.newId()).thenReturn(artifactId);
    when(artifactRepository.save(any(ArtifactEntity.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    // When
    val result = artifactService.createArtifact(
        threadId, runId,
        ArtifactEntity.Kind.PDF,
        ArtifactEntity.Stage.DECK,
        "Apple Marketing Pitch - Revised",
        "application/pdf"
    );

    // Then
    verify(artifactRepository).save(artifactCaptor.capture());
    val savedArtifact = artifactCaptor.getValue();

    assertThat(savedArtifact.getKind()).isEqualTo(ArtifactEntity.Kind.PDF);
    assertThat(savedArtifact.getStage()).isEqualTo(ArtifactEntity.Stage.DECK);
    assertThat(savedArtifact.getTitle()).isEqualTo("Apple Marketing Pitch - Revised");
    assertThat(savedArtifact.getMime()).isEqualTo("application/pdf");
    assertThat(savedArtifact.getStorageKey()).isNull(); // No storage key initially
    assertThat(result).isEqualTo(savedArtifact);
  }
}