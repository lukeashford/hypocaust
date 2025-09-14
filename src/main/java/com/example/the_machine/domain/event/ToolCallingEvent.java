package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ToolCallingEvent.ToolCallingPayload;
import java.util.UUID;

public final class ToolCallingEvent extends ToolEvent<ToolCallingPayload> {

  public ToolCallingEvent(UUID threadId, String content) {
    super(threadId, new ToolCallingPayload(content));
  }

  @Override
  public EventType type() {
    return EventType.TOOL_CALLING;
  }

  public record ToolCallingPayload(String content)
      implements ToolEventPayload {

  }
}