package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ErrorEvent.ErrorEventPayload;
import java.util.UUID;

public final class ErrorEvent extends Event<ErrorEventPayload> {

  public ErrorEvent(UUID threadId, String message) {
    super(threadId, new ErrorEventPayload(message));
  }

  @Override
  public EventType type() {
    return EventType.ERROR;
  }

  public record ErrorEventPayload(String message) implements EventPayload {

  }
}
