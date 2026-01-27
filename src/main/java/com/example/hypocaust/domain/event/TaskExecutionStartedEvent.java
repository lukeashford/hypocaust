package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class TaskExecutionStartedEvent extends TaskExecutionEvent<TaskExecutionStartedEvent.Payload> {

  public TaskExecutionStartedEvent(UUID projectId) {
    super(projectId, new Payload());
  }

  @Override
  public EventType type() {
    return EventType.TASKEXECUTION_STARTED;
  }

  public record Payload() implements TaskExecutionEventPayload {

  }
}
