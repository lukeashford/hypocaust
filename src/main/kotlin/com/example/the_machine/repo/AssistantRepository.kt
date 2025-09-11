package com.example.the_machine.repo

import com.example.the_machine.domain.AssistantEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AssistantRepository : JpaRepository<AssistantEntity, UUID>