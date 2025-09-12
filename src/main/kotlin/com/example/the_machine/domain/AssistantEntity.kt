package com.example.the_machine.domain

import com.example.the_machine.common.JsonElementConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import kotlinx.serialization.json.JsonElement
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
@Table(name = "assistant")
data class AssistantEntity(
  @Column(nullable = false)
  val name: String,

  val systemPrompt: String = "You are a helpful assistant.",

  @Column(nullable = false)
  val model: String,

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @Convert(converter = JsonElementConverter::class)
  val paramsJson: JsonElement
) : BaseEntity() {

  companion object {

    val DEFAULT_ASSISTANT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
  }
}