package com.example.the_machine.service.mapping

import com.example.the_machine.common.UUIDSerializer
import com.example.the_machine.dto.content.ContentBlockDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.mapstruct.Named
import org.springframework.stereotype.Component
import java.util.*

@Component
class JsonConverters(private val json: Json) {

  @Named("blocksFromJson")
  fun blocksFromJson(element: JsonElement?): List<ContentBlockDto> {
    if (element == null || element is JsonNull) {
      return emptyList() // DB NULL or JSON null => empty list in DTO
    }
    if (element !is JsonArray) {
      throw IllegalArgumentException("content_json must be a JSON array")
    }
    return json.decodeFromJsonElement(ListSerializer(ContentBlockDto.serializer()), element)
  }

  @Named("blocksToJson")
  fun blocksToJson(blocks: List<ContentBlockDto>): JsonElement {
    return json.encodeToJsonElement(ListSerializer(ContentBlockDto.serializer()), blocks)
  }

  @Named("uuidsFromJson")
  fun uuidsFromJson(element: JsonElement?): List<UUID> {
    if (element == null || element is JsonNull) {
      return emptyList()
    }
    if (element !is JsonArray) {
      throw IllegalArgumentException("attachments_json must be a JSON array")
    }
    // Use configured UUIDSerializer
    return json.decodeFromJsonElement(ListSerializer(UUIDSerializer), element)
  }

  @Named("uuidsToJson")
  fun uuidsToJson(ids: List<UUID>?): JsonElement? {
    return if (ids == null) {
      null
    } else {
      // Use configured UUIDSerializer
      json.encodeToJsonElement(ListSerializer(UUIDSerializer), ids)
    }
  }
}