package com.example.the_machine.dto.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(ContentBlockTypes.MARKDOWN)
data class MarkdownContentDto(
  val markdown: String
) : ContentBlockDto