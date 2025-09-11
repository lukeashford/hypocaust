package com.example.the_machine.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "run")
data class RunEntity(
  @Id
  val id: UUID? = null,

  @Column(nullable = false)
  val threadId: UUID? = null,

  @Column(nullable = false)
  val assistantId: UUID? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val status: Status? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val kind: Kind? = null,

  val reason: String? = null,

  val startedAt: Instant? = null,

  val completedAt: Instant? = null,

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val usageJson: JsonNode? = null,

  val error: String? = null
) {

  enum class Status {
    QUEUED, RUNNING, REQUIRES_ACTION, COMPLETED, FAILED, CANCELLED
  }

  enum class Kind {
    FULL, PARTIAL
  }
}