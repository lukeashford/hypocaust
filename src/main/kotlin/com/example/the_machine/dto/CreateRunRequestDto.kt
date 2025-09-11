package com.example.the_machine.dto

import java.util.*

data class CreateRunRequestDto(
  val threadId: UUID,
  val assistantId: UUID?,            // nullable -> use default assistant
  val input: MessageCreateRequestDto?   // nullable -> run without new user message
)