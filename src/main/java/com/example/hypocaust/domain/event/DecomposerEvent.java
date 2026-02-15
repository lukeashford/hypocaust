package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.DecomposerEvent.DecomposerEventPayload;
import java.util.UUID;

public abstract sealed class DecomposerEvent<T extends DecomposerEventPayload> extends Event<T>
    permits DecomposerStartedEvent, DecomposerFinishedEvent, DecomposerFailedEvent {

  protected DecomposerEvent(UUID taskExecutionId, T payload) {
    super(taskExecutionId, payload);
  }

  public interface DecomposerEventPayload extends EventPayload {

    String task();
  }
}
