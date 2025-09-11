package com.example.the_machine.dto

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.*

data class RunDto(
  val id: UUID,
  val threadId: UUID,
  val assistantId: UUID,
  val status: RunStatus,
  val kind: RunKind,
  val reason: String,
  val startedAt: Instant,
  val completedAt: Instant,
  val usage: JsonNode,
  val error: String
)