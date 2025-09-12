package com.example.the_machine.dto

import com.example.the_machine.domain.ArtifactEntity
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.util.*

data class ArtifactDto(
  val id: UUID,
  val threadId: UUID,
  val runId: UUID?,
  val kind: ArtifactEntity.Kind,
  val stage: ArtifactEntity.Stage,
  val status: ArtifactEntity.Status,
  val title: String?,
  val mime: String?,
  val url: String?,                  // computed if file-backed
  val content: JsonElement?,                 // structured outputs
  val metadata: JsonElement?,                // dims, duration, etc.
  val createdAt: Instant?,
  val supersededById: UUID?
)