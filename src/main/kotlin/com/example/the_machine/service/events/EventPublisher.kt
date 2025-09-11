package com.example.the_machine.service.events

import com.example.the_machine.dto.EventEnvelopeDto
import org.springframework.stereotype.Service
import java.util.*

@Service
class EventPublisher(
  private val eventLogService: EventLogService,
  private val sseHub: SseHub
) {

  fun publish(e: EventEnvelopeDto) {
    // For direct publish without persistence, just broadcast to live subscribers
    sseHub.broadcast(e.threadId, null, e.type, e.data)
  }

  fun publishAndStore(threadId: UUID, e: EventEnvelopeDto, dedupeKeyOrNull: String?) {
    val eventId = eventLogService.append(
      threadId, e.runId, e.messageId, e.type, e.data, dedupeKeyOrNull
    )
    sseHub.broadcast(threadId, eventId, e.type, e.data)
  }
}