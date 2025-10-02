package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.RunStartedEvent.RunStartedEventPayload;
import java.util.UUID;

public final class RunStartedEvent extends RunEvent<RunStartedEventPayload> {

  public RunStartedEvent(UUID threadId, UUID runId) {
    super(threadId, new RunStartedEventPayload(runId));
  }

  @Override
  public EventType type() {
    return EventType.RUN_STARTED;
  }

  public record RunStartedEventPayload(UUID runId) implements RunEventPayload {

  }
}
