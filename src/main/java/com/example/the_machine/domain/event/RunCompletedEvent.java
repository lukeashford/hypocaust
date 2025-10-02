package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.RunCompletedEvent.RunCompletedEventPayload;
import java.util.UUID;

public final class RunCompletedEvent extends RunEvent<RunCompletedEventPayload> {

  public RunCompletedEvent(UUID threadId, UUID runId) {
    super(threadId, new RunCompletedEventPayload(runId));
  }

  @Override
  public EventType type() {
    return EventType.RUN_COMPLETED;
  }

  public record RunCompletedEventPayload(UUID runId) implements RunEventPayload {

  }
}
