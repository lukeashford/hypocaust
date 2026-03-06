package com.example.hypocaust.service;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.ProjectSnapshot;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.exception.NotFoundException;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskExecutionService {

  private final TaskExecutionRepository taskExecutionRepository;
  private final VersionManagementService versionManagementService;
  private final TodoService todoService;
  private final ArtifactExternalizer artifactExternalizer;

  public ProjectSnapshot getState() {
    return getState(TaskExecutionContextHolder.getContext().getTaskExecutionId());
  }

  public ProjectSnapshot getState(@Nullable UUID taskExecutionId) {
    if (taskExecutionId == null) {
      return new ProjectSnapshot();
    }

    return TaskExecutionContextHolder.getContextByTaskExecutionId(taskExecutionId)
        .map(TaskExecutionContext::getSnapshot)
        .orElseGet(() -> mapFromDatabase(taskExecutionId));
  }

  public ProjectSnapshot getLatestProjectState(@NonNull UUID projectId) {
    UUID latestId = taskExecutionRepository.findTopByProjectIdOrderByStartedAtDesc(projectId)
        .map(TaskExecutionEntity::getId).orElse(null);

    return getState(latestId);
  }

  private ProjectSnapshot mapFromDatabase(UUID taskExecutionId) {
    TaskExecutionEntity entity = taskExecutionRepository.findById(taskExecutionId)
        .orElseThrow(
            () -> new NotFoundException("TaskExecution not found: " + taskExecutionId));

    return new ProjectSnapshot(
        entity.getName(),
        taskExecutionId,
        entity.getStatus(),
        versionManagementService.getAllMaterializedArtifactsAt(taskExecutionId).stream()
            .map(artifactExternalizer::externalize)
            .toList(),
        todoService.getTodosForTaskExecution(taskExecutionId),
        null // Finished tasks don't emit new events
    );
  }
}
