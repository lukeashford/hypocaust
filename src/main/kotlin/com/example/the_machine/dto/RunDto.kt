package com.example.the_machine.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.util.*

@Serializable
data class RunDto(
  @Contextual val id: UUID,
  @Contextual val threadId: UUID,
  @Contextual val assistantId: UUID,
  val status: RunStatus,
  val kind: RunKind,
  val reason: String?,
  @Contextual val startedAt: Instant?,
  @Contextual val completedAt: Instant?,
  val usage: JsonElement?,
  val error: String?
)