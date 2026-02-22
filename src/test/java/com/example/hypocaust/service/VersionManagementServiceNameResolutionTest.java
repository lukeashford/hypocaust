package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactChange;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.domain.TaskExecutionStatus;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VersionManagementServiceNameResolutionTest {

  private TaskExecutionRepository taskExecutionRepository;
  private ArtifactService artifactService;
  private VersionManagementService service;

  private static final UUID PROJECT_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    taskExecutionRepository = mock(TaskExecutionRepository.class);
    artifactService = mock(ArtifactService.class);
    service = new VersionManagementService(taskExecutionRepository, artifactService);
  }

  @Test
  void shouldResolveArtifactByExecutionName() {
    UUID artifactId = UUID.randomUUID();

    // Set up execution with name and delta
    TaskExecutionEntity execution = TaskExecutionEntity.builder()
        .projectId(PROJECT_ID)
        .name("initial_designs")
        .status(TaskExecutionStatus.COMPLETED)
        .build();
    execution.complete("Created initial designs",
        new TaskExecutionDelta(
            List.of(new ArtifactChange("hero_portrait", artifactId)),
            List.of(),
            List.of()
        ));

    // Use reflection or direct field access to set the id
    // Since BaseEntity generates id in constructor, we mock the repository lookup
    when(taskExecutionRepository.findByProjectIdAndName(PROJECT_ID, "initial_designs"))
        .thenReturn(Optional.of(execution));
    when(taskExecutionRepository.findById(execution.getId()))
        .thenReturn(Optional.of(execution));

    Artifact expectedArtifact = Artifact.builder()
        .name("hero_portrait")
        .kind(ArtifactKind.IMAGE)
        .title("Hero Portrait")
        .description("Initial designs for the hero")
        .status(ArtifactStatus.MANIFESTED)
        .build();
    when(artifactService.getArtifact(artifactId)).thenReturn(Optional.of(expectedArtifact));

    Optional<Artifact> result = service.getMaterializedArtifactAtExecution(
        "hero_portrait", "initial_designs", PROJECT_ID);

    assertThat(result).isPresent();
    assertThat(result.get()).isSameAs(expectedArtifact);
  }

  @Test
  void shouldReturnEmptyForUnknownExecutionName() {
    when(taskExecutionRepository.findByProjectIdAndName(PROJECT_ID, "nonexistent"))
        .thenReturn(Optional.empty());

    Optional<Artifact> result = service.getMaterializedArtifactAtExecution(
        "hero_portrait", "nonexistent", PROJECT_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenArtifactNotInSnapshot() {
    TaskExecutionEntity execution = TaskExecutionEntity.builder()
        .projectId(PROJECT_ID)
        .name("some_task")
        .status(TaskExecutionStatus.COMPLETED)
        .build();
    execution.complete("Did something",
        new TaskExecutionDelta(
            List.of(new ArtifactChange("other_artifact", UUID.randomUUID())),
            List.of(),
            List.of()
        ));

    when(taskExecutionRepository.findByProjectIdAndName(PROJECT_ID, "some_task"))
        .thenReturn(Optional.of(execution));
    when(taskExecutionRepository.findById(execution.getId()))
        .thenReturn(Optional.of(execution));

    Optional<Artifact> result = service.getMaterializedArtifactAtExecution(
        "hero_portrait", "some_task", PROJECT_ID);

    assertThat(result).isEmpty();
  }
}
