package com.example.the_machine.dto

import com.example.the_machine.dto.content.ContentBlockDto
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

@Serializable
data class MessageDto(
  val id: @Contextual UUID,
  val threadId: @Contextual UUID,
  val author: AuthorType,
  val createdAt: @Contextual Instant,
  val content: List<ContentBlockDto>,
  val attachments: List<@Contextual UUID>
)