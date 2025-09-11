package com.example.the_machine.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
@Table(name = "assistant")
data class AssistantEntity(
  @Id
  val id: UUID? = null,

  @Column(nullable = false)
  val name: String? = null,

  val systemPrompt: String? = null,

  @Column(nullable = false)
  val model: String? = null,

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val paramsJson: JsonNode? = null
) {

  companion object {

    val DEFAULT_ASSISTANT_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
  }
}