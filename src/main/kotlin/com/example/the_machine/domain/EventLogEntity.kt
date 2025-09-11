package com.example.the_machine.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "event_log")
data class EventLogEntity(
  @Id
  val id: UUID? = null,

  @Column(nullable = false)
  val threadId: UUID? = null,

  val runId: UUID? = null,

  val messageId: UUID? = null,

  @Column(nullable = false)
  val eventType: EventType? = null,

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val payload: JsonNode? = null,

  @Column(nullable = false)
  val occurredAt: Instant? = null,

  val dedupeKey: String? = null
)