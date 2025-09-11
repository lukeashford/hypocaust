package com.example.the_machine.dto.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
@SerialName(ContentBlockTypes.TOOL_RESULT)
data class ToolResultContentDto(
  val name: String,
  val result: JsonElement,
  val callId: String
) : ContentBlockDto