package com.example.the_machine.service.events

import com.example.the_machine.common.IdGenerator
import com.example.the_machine.domain.EventLogEntity
import com.example.the_machine.domain.EventType
import com.example.the_machine.repo.EventLogRepository
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class EventLogService(
  private val eventLogRepository: EventLogRepository,
  private val idGenerator: IdGenerator
) {

  @Transactional
  fun append(
    threadId: UUID,
    runId: UUID?,
    messageId: UUID?,
    eventType: EventType,
    payloadJson: JsonNode,
    dedupeKey: String?
  ): UUID {
    val entity = EventLogEntity(
      id = idGenerator.newId(),
      threadId = threadId,
      runId = runId,
      messageId = messageId,
      eventType = eventType,
      payload = payloadJson,
      occurredAt = Instant.now(),
      dedupeKey = dedupeKey
    )

    val saved = eventLogRepository.save(entity)
    return saved.id!!
  }
}