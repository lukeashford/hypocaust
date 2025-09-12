package com.example.the_machine.common

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement

/**
 * JPA AttributeConverter for JsonElement fields.
 * Converts JsonElement to/from PostgreSQL JSONB for database storage using Kotlin Serialization.
 */
@Converter(autoApply = false)
class JsonElementConverter : AttributeConverter<JsonElement?, String?> {

  override fun convertToDatabaseColumn(attribute: JsonElement?): String? {
    return if (attribute == null) {
      null
    } else {
      KotlinSerializationConfig.staticJson.encodeToString(attribute)
    }
  }

  override fun convertToEntityAttribute(dbData: String?): JsonElement? {
    return if (dbData.isNullOrBlank()) {
      null
    } else {
      KotlinSerializationConfig.staticJson.parseToJsonElement(dbData)
    }
  }
}