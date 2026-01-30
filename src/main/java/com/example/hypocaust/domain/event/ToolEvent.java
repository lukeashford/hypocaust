package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ToolEvent.ToolEventPayload;
import java.util.UUID;

public abstract sealed class ToolEvent<T extends ToolEventPayload> extends Event<T>
    permits ToolCallingEvent {

  protected ToolEvent(UUID taskExecutionId, T payload) {
    super(taskExecutionId, payload);
  }

  public interface ToolEventPayload extends EventPayload {

  }
}
