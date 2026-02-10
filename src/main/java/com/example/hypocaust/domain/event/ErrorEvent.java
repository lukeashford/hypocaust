package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ErrorEvent.ErrorEventPayload;
import java.util.UUID;

public final class ErrorEvent extends Event<ErrorEventPayload> {

  public ErrorEvent(UUID taskExecutionId, String message) {
    super(taskExecutionId, new ErrorEventPayload(message));
  }

  @Override
  public EventType type() {
    return EventType.ERROR;
  }

  public record ErrorEventPayload(String message) implements EventPayload {

  }
}
