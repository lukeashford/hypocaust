package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class TaskExecutionFailedEvent extends
    TaskExecutionEvent<TaskExecutionFailedEvent.Payload> {

  public TaskExecutionFailedEvent(UUID taskExecutionId, String reason) {
    super(taskExecutionId, new Payload(reason));
  }

  @Override
  public EventType type() {
    return EventType.TASKEXECUTION_FAILED;
  }

  public record Payload(String reason) implements TaskExecutionEventPayload {

  }
}
