package com.example.the_machine.domain

import com.example.the_machine.common.JsonElementConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kotlinx.serialization.json.JsonElement
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "event_log")
data class EventLogEntity(

  @Column(nullable = false)
  val threadId: UUID,

  val runId: UUID?,

  val messageId: UUID?,

  @Column(nullable = false)
  val eventType: EventType,

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = JsonElementConverter::class)
  val payload: JsonElement,

  @Column(nullable = false)
  val occurredAt: Instant,

  val dedupeKey: String?
) : BaseEntity()