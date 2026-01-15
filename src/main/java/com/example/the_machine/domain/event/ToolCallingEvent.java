package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ToolCallingEvent.ToolCallingEventPayload;
import java.util.UUID;

public final class ToolCallingEvent extends ToolEvent<ToolCallingEventPayload> {

  public ToolCallingEvent(UUID projectId, String content) {
    super(projectId, new ToolCallingEventPayload(content));
  }

  @Override
  public EventType type() {
    return EventType.TOOL_CALLING;
  }

  public record ToolCallingEventPayload(String content) implements ToolEventPayload {

  }
}