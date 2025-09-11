package com.example.the_machine.domain

import com.example.the_machine.service.mapping.Default
import jakarta.persistence.*
import kotlinx.serialization.json.JsonElement
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "message")
data class MessageEntity @Default constructor(
  @Id
  val id: UUID? = null,

  @Column(nullable = false)
  val threadId: UUID? = null,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val author: Author? = null,

  @Column(nullable = false)
  val createdAt: Instant? = null,

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val contentJson: JsonElement? = null,

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val attachmentsJson: JsonElement? = null
) {

  enum class Author {
    USER, ASSISTANT, TOOL, SYSTEM
  }

}