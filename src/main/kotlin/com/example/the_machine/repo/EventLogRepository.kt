package com.example.the_machine.repo

import com.example.the_machine.domain.EventLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EventLogRepository : JpaRepository<EventLogEntity, UUID> {

  fun existsByIdAndThreadId(id: UUID, threadId: UUID): Boolean
  fun findByThreadIdAndIdGreaterThanOrderById(threadId: UUID, id: UUID): List<EventLogEntity>
}