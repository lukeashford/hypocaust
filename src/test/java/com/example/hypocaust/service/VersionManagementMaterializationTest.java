package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.Changelist;
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
  void materialize_someFail_reportsAnyFailed() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID executionId = UUID.randomUUID();
    Artifact a1 = Artifact.builder()
        .name("a1")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.CREATED)
        .title("T1")
        .description("D1")
        .build();
    Artifact a2 = Artifact.builder()
        .name("a2")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.CREATED)
        .title("T2")
        .description("D2")
        .build();

    Changelist changelist = new Changelist();
    changelist.addArtifact(a1);
    changelist.addArtifact(a2);

    when(artifactService.materialize(eq(a1), any(), any()))
        .thenReturn(a1.withStatus(ArtifactStatus.MANIFESTED));
    when(artifactService.materialize(eq(a2), any(), any()))
        .thenReturn(a2.withStatus(ArtifactStatus.FAILED));

    // When
    VersionManagementService.MaterializationResult result =
        versionManagementService.materialize(changelist, executionId, projectId);

    // Then
    assertThat(result.anyFailed()).isTrue();
    assertThat(result.allFailed()).isFalse();
    assertThat(result.delta().added()).hasSize(2);
  }

  @Test
  void materialize_allFail_reportsAllFailed() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID executionId = UUID.randomUUID();
    Artifact a1 = Artifact.builder()
        .name("a1")
        .kind(ArtifactKind.IMAGE)
        .status(ArtifactStatus.CREATED)
        .title("T1")
        .description("D1")
        .build();

    Changelist changelist = new Changelist();
    changelist.addArtifact(a1);

    when(artifactService.materialize(eq(a1), any(), any()))
        .thenReturn(a1.withStatus(ArtifactStatus.FAILED));

    // When
    VersionManagementService.MaterializationResult result =
        versionManagementService.materialize(changelist, executionId, projectId);

    // Then
    assertThat(result.anyFailed()).isTrue();
    assertThat(result.allFailed()).isTrue();
  }
}
