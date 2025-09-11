package com.example.the_machine.repo

import com.example.the_machine.domain.ArtifactEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ArtifactRepository : JpaRepository<ArtifactEntity, UUID> {

  fun findByThreadIdOrderByCreatedAtDesc(threadId: UUID): List<ArtifactEntity>
}