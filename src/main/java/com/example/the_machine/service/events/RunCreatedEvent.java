package com.example.the_machine.service.events;

import java.util.UUID;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event published when a run is created and ready for execution. Used with
 * {@link TransactionalEventListener} to ensure async execution starts only after the transaction
 * commits.
 */
public record RunCreatedEvent(UUID runId, UUID threadId) {

}