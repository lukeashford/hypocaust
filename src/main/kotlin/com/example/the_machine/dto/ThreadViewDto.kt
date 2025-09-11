package com.example.the_machine.dto

data class ThreadViewDto(
  val thread: ThreadDto,
  val messages: List<MessageDto>,
  val artifacts: List<ArtifactDto>,
  val latestRun: RunDto?
)