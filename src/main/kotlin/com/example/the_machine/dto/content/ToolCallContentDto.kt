package com.example.the_machine.dto.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
@SerialName(ContentBlockTypes.TOOL_CALL)
data class ToolCallContentDto(
  val name: String,
  val arguments: JsonElement
) : ContentBlockDto