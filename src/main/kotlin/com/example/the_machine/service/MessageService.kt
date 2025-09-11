package com.example.the_machine.service

import com.example.the_machine.domain.MessageEntity
import com.example.the_machine.dto.CreateRunRequestDto
import com.example.the_machine.dto.MessageCreateRequestDto
import com.example.the_machine.dto.RunDto
import com.example.the_machine.repo.MessageRepository
import com.example.the_machine.service.mapping.JsonConverters
import kotlinx.serialization.json.JsonElement
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Service for handling message creation and processing.
 */
@Service
class MessageService(
  private val messageRepository: MessageRepository,
  private val runService: RunService,
  private val jsonConverters: JsonConverters
) {

  /**
   * Processes a message request by creating a user message and triggering a run.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the created run DTO
   */
  @Transactional
  fun processMessage(threadId: UUID, request: MessageCreateRequestDto): RunDto {
    log.info { "Processing message for thread: $threadId" }

    // Create and persist user message
    createUserMessage(threadId, request)

    // Create run request using record constructor
    val runRequest = CreateRunRequestDto(threadId, null, request)

    // Trigger run execution
    val runDto = runService.createRun(runRequest)

    log.info { "Message processed and run created: ${runDto.id} for thread: $threadId" }
    return runDto
  }

  /**
   * Creates a user message entity.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the created message entity
   */
  private fun createUserMessage(threadId: UUID, request: MessageCreateRequestDto): MessageEntity {
    // Convert AuthorType to MessageEntity.Author
    val author = MessageEntity.Author.valueOf(request.author.name)

    // Convert content to JsonElement
    val contentJson: JsonElement? = jsonConverters.blocksToJson(request.content)
    val attachmentsJson: JsonElement? = jsonConverters.uuidsToJson(request.attachments)

    val message = MessageEntity(
      id = UUID.randomUUID(),
      threadId = threadId,
      author = author,
      contentJson = contentJson,
      attachmentsJson = attachmentsJson,
      createdAt = Instant.now()
    )

    return messageRepository.save(message)
  }
}