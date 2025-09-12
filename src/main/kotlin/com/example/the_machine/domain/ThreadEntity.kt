package com.example.the_machine.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "thread")
data class ThreadEntity(
  val title: String = "",

  @Column(nullable = false)
  val lastActivityAt: Instant
) : BaseEntity()