package com.example.the_machine.dto

import com.example.the_machine.domain.ArtifactEntity
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.*

data class ArtifactDto(
  val id: UUID,
  val threadId: UUID,
  val runId: UUID,
  val kind: ArtifactEntity.Kind,
  val stage: ArtifactEntity.Stage,
  val status: ArtifactEntity.Status,
  val title: String,
  val mime: String,
  val url: String,                  // computed if file-backed
  val content: JsonNode,            // structured outputs
  val metadata: JsonNode,           // dims, duration, etc.
  val createdAt: Instant,
  val supersededById: UUID
)