package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class TaskExecutionCompletedEvent extends
    TaskExecutionEvent<TaskExecutionCompletedEvent.Payload> {

  public TaskExecutionCompletedEvent(UUID taskExecutionId, boolean hasChanges, String name,
      String message) {
    super(taskExecutionId, new Payload(hasChanges, name, message));
  }

  @Override
  public EventType type() {
    return EventType.TASKEXECUTION_COMPLETED;
  }

  public record Payload(boolean hasChanges, String name,
                         String message) implements TaskExecutionEventPayload {

  }
}
