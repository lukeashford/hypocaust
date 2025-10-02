package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ToolEvent.ToolEventPayload;
import java.util.UUID;

public abstract sealed class ToolEvent<T extends ToolEventPayload> extends
    Event<ToolEventPayload>
    permits ToolCallingEvent {

  protected ToolEvent(UUID threadId, T payload) {
    super(threadId, payload);
  }

  public interface ToolEventPayload extends EventPayload {

  }
}
