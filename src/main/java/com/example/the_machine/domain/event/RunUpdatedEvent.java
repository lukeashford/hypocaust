package com.example.the_machine.domain.event;

import com.example.the_machine.db.RunEntity.Status;
import com.example.the_machine.domain.event.RunUpdatedEvent.RunUpdatedPayload;
import java.util.UUID;

public final class RunUpdatedEvent extends RunEvent<RunUpdatedPayload> {

  public RunUpdatedEvent(
      UUID threadId, UUID runId, UUID assistantId, Status status, String reason
  ) {
    super(threadId, new RunUpdatedPayload(runId, assistantId, status, reason));
  }

  @Override
  public EventType type() {
    return EventType.RUN_UPDATED;
  }

  public record RunUpdatedPayload(
      UUID runId,
      UUID assistantId,
      Status status,
      String reason
  ) implements RunEventPayload {

  }
}