package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.hypocaust.domain.Changelist;
import com.example.hypocaust.repo.TaskExecutionRepository;
import org.junit.jupiter.api.Test;

class VersionManagementServiceTest {

  @Test
  void getAllArtifactsWithChanges_withNullTaskExecutionId_shouldNotThrowNpe() {
    // Given
    TaskExecutionRepository repo = mock(TaskExecutionRepository.class);
    ArtifactService artifactService = mock(ArtifactService.class);
    VersionManagementService service = new VersionManagementService(repo, artifactService);
    Changelist changelist = new Changelist();

    // When & Then (should not throw NullPointerException)
    var result = service.getAllArtifactsWithChanges(null, changelist);
    assertThat(result).isEmpty();
  }
}
