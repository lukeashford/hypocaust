package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.TaskTree;
import java.util.UUID;

public final class TaskProgressUpdatedEvent extends TaskProgressEvent<TaskProgressUpdatedEvent.Payload> {

  public TaskProgressUpdatedEvent(UUID projectId, TaskTree taskTree) {
    super(projectId, new Payload(taskTree));
  }

  @Override
  public EventType type() {
    return EventType.TASK_PROGRESS_UPDATED;
  }

  public record Payload(TaskTree taskTree) implements TaskProgressEventPayload {

  }
}
