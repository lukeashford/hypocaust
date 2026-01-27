package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class TaskExecutionCompletedEvent extends TaskExecutionEvent<TaskExecutionCompletedEvent.Payload> {

  public TaskExecutionCompletedEvent(UUID projectId, boolean hasChanges, String message) {
    super(projectId, new Payload(hasChanges, message));
  }

  @Override
  public EventType type() {
    return EventType.TASKEXECUTION_COMPLETED;
  }

  public record Payload(boolean hasChanges, String message) implements TaskExecutionEventPayload {

  }
}
