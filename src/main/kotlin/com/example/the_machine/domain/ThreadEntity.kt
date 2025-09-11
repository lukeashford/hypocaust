package com.example.the_machine.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.*

@Entity
@Table(name = "thread")
data class ThreadEntity(
  @Id
  val id: UUID? = null,

  val title: String? = null,

  @Column(nullable = false)
  val createdAt: Instant? = null,

  @Column(nullable = false)
  val lastActivityAt: Instant? = null
)