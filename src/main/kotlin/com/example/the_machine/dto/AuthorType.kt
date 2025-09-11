package com.example.the_machine.dto

import kotlinx.serialization.Serializable

@Serializable
enum class AuthorType {

  USER,
  ASSISTANT,
  TOOL,
  SYSTEM
}