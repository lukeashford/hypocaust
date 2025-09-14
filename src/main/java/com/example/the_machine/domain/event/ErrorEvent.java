package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ErrorEvent.ErrorPayload;
import java.util.UUID;

public final class ErrorEvent extends Event<ErrorPayload> {

  public ErrorEvent(UUID threadId, String message) {
    super(threadId, new ErrorPayload(message));
  }

  @Override
  public EventType type() {
    return EventType.ERROR;
  }

  public record ErrorPayload(String message) implements Payload {

  }
}
