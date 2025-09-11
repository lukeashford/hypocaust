package com.example.the_machine.dto

import java.time.Instant
import java.util.*

data class ThreadDto(
  val id: UUID,
  val title: String,
  val createdAt: Instant,
  val lastActivityAt: Instant
)