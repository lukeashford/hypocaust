package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class DecomposerFailedEvent extends DecomposerEvent<DecomposerFailedEvent.Payload> {

  public DecomposerFailedEvent(UUID taskExecutionId, String task, String reason) {
    super(taskExecutionId, new Payload(task, reason));
  }

  @Override
  public EventType type() {
    return EventType.DECOMPOSER_FAILED;
  }

  public record Payload(String task, String reason) implements DecomposerEventPayload {

  }
}
