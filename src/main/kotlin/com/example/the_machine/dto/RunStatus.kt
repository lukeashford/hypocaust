package com.example.the_machine.dto

import kotlinx.serialization.Serializable

@Serializable
enum class RunStatus {

  QUEUED,
  RUNNING,
  REQUIRES_ACTION,
  COMPLETED,
  FAILED,
  CANCELLED;

}