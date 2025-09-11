package com.example.the_machine.repo

import com.example.the_machine.domain.ThreadEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ThreadRepository : JpaRepository<ThreadEntity, UUID>