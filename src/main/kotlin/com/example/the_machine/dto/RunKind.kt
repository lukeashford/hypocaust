package com.example.the_machine.dto

import kotlinx.serialization.Serializable

@Serializable
enum class RunKind {

  FULL,
  PARTIAL;

}