package com.example.the_machine.repo

import com.example.the_machine.domain.MessageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MessageRepository : JpaRepository<MessageEntity, UUID> {

  fun findByThreadIdOrderByCreatedAt(threadId: UUID): List<MessageEntity>
}