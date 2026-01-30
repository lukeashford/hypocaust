package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ToolCallingEvent.ToolCallingEventPayload;
import java.util.UUID;

public final class ToolCallingEvent extends ToolEvent<ToolCallingEventPayload> {

  public ToolCallingEvent(UUID taskExecutionId, String content) {
    super(taskExecutionId, new ToolCallingEventPayload(content));
  }

  @Override
  public EventType type() {
    return EventType.TOOL_CALLING;
  }

  public record ToolCallingEventPayload(String content) implements ToolEventPayload {

  }
}