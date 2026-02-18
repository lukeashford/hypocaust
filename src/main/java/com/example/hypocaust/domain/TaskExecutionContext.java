package com.example.hypocaust.domain;

import com.example.hypocaust.service.NamingService;
import com.example.hypocaust.service.VersionManagementService;
import com.example.hypocaust.service.events.EventService;
import java.util.UUID;
import lombok.Getter;

/**
 * Thread-local context for the current TaskExecution. Replaces both RunContextHolder and
 * ExecutionContext with a unified approach. Also incorporates task progress tracking (previously in
 * TaskProgressService).
 * <p>
 * Refactored into a Sub-Context Architecture to delegate artifact and todo management.
 */
@Getter
public class TaskExecutionContext {

  private final UUID projectId;
  private final UUID taskExecutionId;
  private final UUID predecessorId;
  private final String name;

  private final ArtifactsContext artifacts;
  private final TodosContext todos;

  @Getter
  private volatile UUID lastEventId;

  public TaskExecutionContext(
      UUID projectId,
      UUID taskExecutionId,
      UUID predecessorId,
      String name,
      EventService eventService,
      VersionManagementService versionService,
      NamingService namingService) {
    this.projectId = projectId;
    this.taskExecutionId = taskExecutionId;
    this.predecessorId = predecessorId;
    this.name = name;

    this.artifacts = new ArtifactsContext(
        projectId, taskExecutionId, predecessorId,
        eventService, versionService, namingService
    );
    this.todos = new TodosContext(taskExecutionId, eventService);
  }

  public void updateLastEventId(UUID eventId) {
    this.lastEventId = eventId;
  }

  public synchronized TaskExecutionSnapshot getSnapshot() {
    return new TaskExecutionSnapshot(
        taskExecutionId,
        name,
        TaskExecutionStatus.RUNNING,
        artifacts.getAllWithChanges(),
        todos.getList().toList(),
        lastEventId
    );
  }
}
