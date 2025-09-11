package com.example.the_machine.service.events

import com.example.the_machine.repo.EventLogRepository
import com.example.the_machine.repo.ThreadRepository
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*

@Service
class EventService(
  private val threadRepository: ThreadRepository,
  private val eventLogRepository: EventLogRepository,
  private val sseHub: SseHub
) {

  private val log = KotlinLogging.logger {}

  fun subscribeToEvents(threadId: UUID, lastEventIdFromHeader: String?): SseEmitter {
    // 1. Validate thread exists
    threadRepository.findById(threadId).orElseThrow {
      ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found: $threadId")
    }

    // 2. Parse and validate lastEventId
    val lastEventId = parseAndValidateLastEventId(lastEventIdFromHeader, threadId)

    // 3. Delegate to SseHub
    log.debug { "SSE subscription for thread $threadId with validated lastEventId: $lastEventId" }
    return sseHub.subscribe(threadId, lastEventId)
  }

  private fun parseAndValidateLastEventId(header: String?, threadId: UUID): UUID? {
    if (header.isNullOrBlank()) {
      return null
    }

    val lastEventId: UUID
    try {
      lastEventId = UUID.fromString(header.trim())
    } catch (e: IllegalArgumentException) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Invalid Last-Event-ID format: $header"
      )
    }

    // Validate the event exists for this thread
    if (!eventLogRepository.existsByIdAndThreadId(lastEventId, threadId)) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Last-Event-ID not found for thread: $lastEventId"
      )
    }

    return lastEventId
  }
}