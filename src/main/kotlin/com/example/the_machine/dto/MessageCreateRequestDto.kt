package com.example.the_machine.dto

import com.example.the_machine.dto.content.ContentBlockDto
import java.util.*

data class MessageCreateRequestDto(
  val author: AuthorType,
  val content: List<ContentBlockDto>,
  val attachments: List<UUID>? = null
)