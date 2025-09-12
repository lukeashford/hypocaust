package com.example.the_machine.domain

import com.example.the_machine.common.UuidV7
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.*

@MappedSuperclass
abstract class BaseEntity {

  @Id
  val id: UUID = UuidV7.newId()

  @Column(nullable = false, updatable = false)
  @CreationTimestamp
  val createdAt: Instant? = null

}