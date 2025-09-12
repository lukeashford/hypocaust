package com.example.the_machine.domain

import com.example.the_machine.common.JsonElementConverter
import jakarta.persistence.*
import kotlinx.serialization.json.JsonElement
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "run")
data class RunEntity(
  @Column(nullable = false)
  val threadId: UUID,

  @Column(nullable = false)
  val assistantId: UUID,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val status: Status,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val kind: Kind,

  val reason: String? = null,

  val startedAt: Instant? = null,

  val completedAt: Instant? = null,

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = JsonElementConverter::class)
  val usageJson: JsonElement? = null,

  val error: String? = null
) : BaseEntity() {

  enum class Status {
    QUEUED, RUNNING, REQUIRES_ACTION, COMPLETED, FAILED, CANCELLED
  }

  enum class Kind {
    FULL, PARTIAL
  }
}