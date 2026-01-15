package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ToolEvent.ToolEventPayload;
import java.util.UUID;

public abstract sealed class ToolEvent<T extends ToolEventPayload> extends Event<T>
    permits ToolCallingEvent {

  protected ToolEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface ToolEventPayload extends EventPayload {

  }
}
