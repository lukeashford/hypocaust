package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.RunStartedEvent.RunStartedEventPayload;
import java.util.UUID;

public final class RunStartedEvent extends RunEvent<RunStartedEventPayload> {

  public RunStartedEvent(UUID projectId, UUID runId) {
    super(projectId, new RunStartedEventPayload(runId));
  }

  @Override
  public EventType type() {
    return EventType.RUN_STARTED;
  }

  public record RunStartedEventPayload(UUID runId) implements RunEventPayload {

  }
}
