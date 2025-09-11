package com.example.the_machine.domain

import kotlinx.serialization.Serializable

@Serializable
enum class EventType(val serialName: String) {

  RUN_CREATED("run.created"),

  RUN_UPDATED("run.updated"),

  MESSAGE_DELTA("message.delta"),

  MESSAGE_COMPLETED("message.completed"),

  ARTIFACT_CREATED("artifact.created"),

  ERROR("error")
}