package com.example.hypocaust.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ProjectSnapshot;
import com.example.hypocaust.domain.TaskExecutionStatus;
import com.example.hypocaust.exception.NotFoundException;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskExecutionServiceTest {

  @Mock
  private TaskExecutionRepository taskExecutionRepository;

  @Mock
  private VersionManagementService versionManagementService;

  @Mock
  private TodoService todoService;

  @InjectMocks
  private TaskExecutionService taskExecutionService;

  @Test
  void getLatestProjectState_projectFound_returnsSnapshot() {
    // Given
    UUID projectId = UUID.randomUUID();
    UUID executionId = UUID.randomUUID();
    TaskExecutionEntity entity = mock(TaskExecutionEntity.class);
    when(entity.getId()).thenReturn(executionId);
    when(entity.getName()).thenReturn("test-name");
    when(entity.getStatus()).thenReturn(TaskExecutionStatus.COMPLETED);

    when(taskExecutionRepository.findTopByProjectIdOrderByStartedAtDesc(projectId))
        .thenReturn(Optional.of(entity));
    when(taskExecutionRepository.findById(executionId)).thenReturn(Optional.of(entity));
    when(versionManagementService.getAllMaterializedArtifactsAt(executionId)).thenReturn(List.of());
    when(todoService.getTodosForTaskExecution(executionId)).thenReturn(List.of());

    // When
    ProjectSnapshot snapshot = taskExecutionService.getLatestProjectState(projectId);

    // Then
    assertThat(snapshot.taskExecutionId()).isEqualTo(executionId);
    assertThat(snapshot.name()).isEqualTo("test-name");
    assertThat(snapshot.status()).isEqualTo(TaskExecutionStatus.COMPLETED);
  }

  @Test
  void getLatestProjectState_noExecutions_returnsEmptySnapshot() {
    // Given
    UUID projectId = UUID.randomUUID();
    when(taskExecutionRepository.findTopByProjectIdOrderByStartedAtDesc(projectId))
        .thenReturn(Optional.empty());

    // When
    ProjectSnapshot snapshot = taskExecutionService.getLatestProjectState(projectId);

    // Then
    assertThat(snapshot.taskExecutionId()).isNull();
    assertThat(snapshot.name()).isNull();
    assertThat(snapshot.status()).isNull();
    assertThat(snapshot.artifacts()).isEmpty();
    assertThat(snapshot.todos()).isEmpty();
    assertThat(snapshot.lastEventId()).isNull();
  }

  @Test
  void getState_executionNotFound_throwsNotFoundException() {
    // Given
    UUID executionId = UUID.randomUUID();
    when(taskExecutionRepository.findById(executionId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> taskExecutionService.getState(executionId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("TaskExecution not found");
  }
}
