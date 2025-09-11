package com.example.the_machine.dto.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(ContentBlockTypes.TEXT)
data class TextContentDto(
  val text: String
) : ContentBlockDto