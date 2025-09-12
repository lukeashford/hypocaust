package com.example.the_machine.service.events

import com.example.the_machine.domain.EventLogEntity
import com.example.the_machine.domain.EventType
import com.example.the_machine.repo.EventLogRepository
import kotlinx.serialization.json.JsonElement
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class EventLogService(
  private val eventLogRepository: EventLogRepository,
) {

  @Transactional
  fun append(
    threadId: UUID,
    runId: UUID?,
    messageId: UUID?,
    eventType: EventType,
    payloadJson: JsonElement,
    dedupeKey: String?
  ): UUID {
    val entity = EventLogEntity(
      threadId = threadId,
      runId = runId,
      messageId = messageId,
      eventType = eventType,
      payload = payloadJson,
      occurredAt = Instant.now(),
      dedupeKey = dedupeKey
    )

    val saved = eventLogRepository.save(entity)
    return saved.id
  }
}