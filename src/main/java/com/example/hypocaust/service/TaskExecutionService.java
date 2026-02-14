package com.example.hypocaust.service;

import com.example.hypocaust.db.TaskExecutionEntity;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TaskExecutionSnapshot;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.repo.TaskExecutionRepository;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskExecutionService {

  private final TaskExecutionRepository taskExecutionRepository;
  private final VersionManagementService versionManagementService;
  private final TodoService todoService;

  public TaskExecutionSnapshot getState() {
    return getState(TaskExecutionContextHolder.getContext().getTaskExecutionId());
  }

  public TaskExecutionSnapshot getState(@NonNull UUID taskExecutionId) {
    return TaskExecutionContextHolder.getContextByTaskExecutionId(taskExecutionId)
        .map(TaskExecutionContext::getSnapshot)
        .orElseGet(() -> mapFromDatabase(taskExecutionId));
  }

  private TaskExecutionSnapshot mapFromDatabase(UUID id) {
    TaskExecutionEntity entity = taskExecutionRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("TaskExecution not found: " + id));

    return new TaskExecutionSnapshot(
        id,
        entity.getStatus(),
        versionManagementService.getAllMaterializedArtifactsAt(id),
        todoService.getTodosForTaskExecution(id),
        null // Finished tasks don't emit new events
    );
  }
}
