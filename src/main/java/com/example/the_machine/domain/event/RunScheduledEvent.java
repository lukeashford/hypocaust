package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.RunScheduledEvent.RunScheduledEventPayload;
import java.util.UUID;

public final class RunScheduledEvent extends RunEvent<RunScheduledEventPayload> {

  public RunScheduledEvent(UUID projectId, UUID runId) {
    super(projectId, new RunScheduledEventPayload(runId));
  }

  @Override
  public EventType type() {
    return EventType.RUN_SCHEDULED;
  }

  public record RunScheduledEventPayload(UUID runId) implements RunEventPayload {

  }
}
