package com.example.the_machine.repo

import com.example.the_machine.domain.RunEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RunRepository : JpaRepository<RunEntity, UUID> {

  fun findTopByThreadIdOrderByStartedAtDesc(threadId: UUID): RunEntity?
}