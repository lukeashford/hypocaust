package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.Changelist;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionManagementMaterializationTest {

  private TaskExecutionRepository taskExecutionRepository;
  private ArtifactService artifactService;
  private VersionManagementService versionManagementService;

  @BeforeEach
  void setUp() {
    taskExecutionRepository = mock(TaskExecutionRepository.class);
    artifactService = mock(ArtifactService.class);
    versionManagementService = new VersionManagementService(taskExecutionRepository,
        artifactService);
  }

  @Test
  void persist_addedArtifacts_returnsCorrectDelta() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID executionId = UUID.randomUUID();
    UUID artifactId1 = UUID.randomUUID();
    UUID artifactId2 = UUID.randomUUID();

    Artifact a1 = Artifact.builder()
        .name("a1")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/a1.png")
        .title("T1")
        .description("D1")
        .build();
    Artifact a2 = Artifact.builder()
        .name("a2")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.FAILED)
        .errorMessage("Download failed")
        .title("T2")
        .description("D2")
        .build();

    Changelist changelist = new Changelist();
    changelist.addArtifact(a1);
    changelist.addArtifact(a2);

    // persist returns artifact with ID set
    when(artifactService.persist(eq(a1), eq(projectId), eq(executionId)))
        .thenReturn(Artifact.builder().id(artifactId1).name("a1").kind(ArtifactKind.IMAGE)
            .status(ArtifactStatus.MANIFESTED).title("T1").description("D1").build());
    when(artifactService.persist(eq(a2), eq(projectId), eq(executionId)))
        .thenReturn(Artifact.builder().id(artifactId2).name("a2").kind(ArtifactKind.IMAGE)
            .status(ArtifactStatus.FAILED).title("T2").description("D2").build());

    // When
    TaskExecutionDelta delta = versionManagementService.persist(changelist, executionId, projectId);

    // Then
    assertThat(delta.added()).hasSize(2);
    assertThat(delta.edited()).isEmpty();
    assertThat(delta.deleted()).isEmpty();
    verify(artifactService).persist(eq(a1), eq(projectId), eq(executionId));
    verify(artifactService).persist(eq(a2), eq(projectId), eq(executionId));
  }

  @Test
  void persist_noChanges_returnsEmptyDelta() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID executionId = UUID.randomUUID();
    Changelist changelist = new Changelist();

    // When
    TaskExecutionDelta delta = versionManagementService.persist(changelist, executionId, projectId);

    // Then
    assertThat(delta.hasChanges()).isFalse();
  }
}
