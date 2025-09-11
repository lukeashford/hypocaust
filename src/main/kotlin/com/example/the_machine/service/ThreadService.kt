package com.example.the_machine.service

import com.example.the_machine.common.IdGenerator
import com.example.the_machine.domain.ThreadEntity
import com.example.the_machine.dto.ThreadDto
import com.example.the_machine.dto.ThreadViewDto
import com.example.the_machine.repo.ArtifactRepository
import com.example.the_machine.repo.MessageRepository
import com.example.the_machine.repo.RunRepository
import com.example.the_machine.repo.ThreadRepository
import com.example.the_machine.service.mapping.ArtifactMapper
import com.example.the_machine.service.mapping.MessageMapper
import com.example.the_machine.service.mapping.RunMapper
import com.example.the_machine.service.mapping.ThreadMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class ThreadService(
  private val threadRepository: ThreadRepository,
  private val messageRepository: MessageRepository,
  private val artifactRepository: ArtifactRepository,
  private val runRepository: RunRepository,
  private val idGenerator: IdGenerator,
  private val threadMapper: ThreadMapper,
  private val messageMapper: MessageMapper,
  private val artifactMapper: ArtifactMapper,
  private val runMapper: RunMapper
) {

  @Transactional
  fun createThread(): ThreadDto {
    val now = Instant.now()
    val thread = ThreadEntity(
      id = idGenerator.newId(),
      createdAt = now,
      lastActivityAt = now
    )

    val savedThread = threadRepository.save(thread)
    return threadMapper.toDto(savedThread)
  }

  @Transactional(readOnly = true)
  fun getThreadView(threadId: UUID): ThreadViewDto {
    val thread = threadRepository.findById(threadId)
      .orElseThrow { RuntimeException("Thread not found: $threadId") }

    val threadDto = threadMapper.toDto(thread)

    val messages = messageRepository.findByThreadIdOrderByCreatedAt(threadId)
    val messageDtos = messages
      .map { messageMapper.toDto(it) }
      .toList()

    val artifacts = artifactRepository.findByThreadIdOrderByCreatedAtDesc(threadId)
    val artifactDtos = artifacts
      .map { artifactMapper.toDto(it) }
      .toList()

    val latestRun = runRepository.findTopByThreadIdOrderByStartedAtDesc(threadId)
    val latestRunDto = latestRun?.let { runMapper.toDto(it) }

    return ThreadViewDto(threadDto, messageDtos, artifactDtos, latestRunDto)
  }
}