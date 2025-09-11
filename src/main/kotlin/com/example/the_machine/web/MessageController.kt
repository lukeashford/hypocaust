package com.example.the_machine.web

import com.example.the_machine.common.Routes
import com.example.the_machine.dto.MessageCreateRequestDto
import com.example.the_machine.dto.RunDto
import com.example.the_machine.service.MessageService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.concurrent.RejectedExecutionException

/**
 * Controller for handling message processing requests.
 */
@RestController
class MessageController(
  private val messageService: MessageService
) {

  private val log = LoggerFactory.getLogger(MessageController::class.java)

  /**
   * Processes a message and creates a run.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the created run DTO
   */
  @PostMapping(Routes.THREAD_MESSAGES)
  fun processMessage(
    @PathVariable threadId: UUID,
    @RequestBody request: MessageCreateRequestDto
  ): ResponseEntity<RunDto> {

    log.info("Processing message for thread: {}", threadId)

    return try {
      val run = messageService.processMessage(threadId, request)
      ResponseEntity.ok(run)
    } catch (e: RejectedExecutionException) {
      log.warn("Execution rejected for thread {}: {}", threadId, e.message)
      ResponseEntity.status(429).build() // Too Many Requests
    } catch (e: IllegalArgumentException) {
      log.warn("Invalid request for thread: {} - {}", threadId, e.message)
      ResponseEntity.badRequest().build()
    } catch (e: Exception) {
      log.error("Error processing message for thread: {}", threadId, e)
      ResponseEntity.internalServerError().build()
    }
  }
}