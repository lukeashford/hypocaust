package com.example.the_machine.dto

import com.example.the_machine.domain.EventType
import kotlinx.serialization.json.JsonElement
import java.util.*

data class EventEnvelopeDto(
  val type: EventType,
  val threadId: UUID,
  val runId: UUID?,
  val messageId: UUID?,
  val data: JsonElement
)