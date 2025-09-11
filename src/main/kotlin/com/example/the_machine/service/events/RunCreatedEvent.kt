package com.example.the_machine.service.events

import org.springframework.transaction.event.TransactionalEventListener
import java.util.*

/**
 * Event published when a run is created and ready for execution. Used with
 * [TransactionalEventListener] to ensure async execution starts only after the transaction
 * commits.
 */
data class RunCreatedEvent(
  val runId: UUID,
  val threadId: UUID
)