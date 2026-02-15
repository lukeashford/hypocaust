package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class DecomposerStartedEvent extends DecomposerEvent<DecomposerStartedEvent.Payload> {

  public DecomposerStartedEvent(UUID taskExecutionId, String task) {
    super(taskExecutionId, new Payload(task));
  }

  @Override
  public EventType type() {
    return EventType.DECOMPOSER_STARTED;
  }

  public record Payload(String task) implements DecomposerEventPayload {

  }
}
