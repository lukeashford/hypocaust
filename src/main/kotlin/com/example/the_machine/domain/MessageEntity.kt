package com.example.the_machine.domain

import com.example.the_machine.common.JsonElementConverter
import com.example.the_machine.common.KotlinSerializationConfig
import jakarta.persistence.*
import kotlinx.serialization.json.JsonElement
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
@Table(name = "message")
data class MessageEntity(
  @Column(nullable = false)
  val threadId: UUID,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val author: Author,

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = JsonElementConverter::class)
  val contentJson: JsonElement = KotlinSerializationConfig.staticJson.parseToJsonElement("[]"),

  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = JsonElementConverter::class)
  val attachmentsJson: JsonElement? = null
) : BaseEntity() {

  enum class Author {
    USER, ASSISTANT, TOOL, SYSTEM
  }

}