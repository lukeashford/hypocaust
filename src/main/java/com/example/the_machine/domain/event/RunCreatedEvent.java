package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.RunCreatedEvent.RunCreatedPayload;
import java.util.UUID;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event published when a run is created and ready for execution. Used with
 * {@link TransactionalEventListener} to ensure async execution starts only after the transaction
 * commits.
 */
public final class RunCreatedEvent extends RunEvent<RunCreatedPayload> {

  public RunCreatedEvent(UUID threadId, UUID runId) {
    super(threadId, new RunCreatedPayload(runId));
  }

  @Override
  public EventType type() {
    return EventType.RUN_CREATED;
  }

  public record RunCreatedPayload(UUID runId) implements RunEventPayload {

  }

}