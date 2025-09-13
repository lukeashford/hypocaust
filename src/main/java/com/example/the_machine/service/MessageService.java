package com.example.the_machine.service;

import com.example.the_machine.domain.MessageEntity;
import com.example.the_machine.dto.CreateRunRequestDto;
import com.example.the_machine.dto.MessageCreateRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.repo.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling message creation and processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

  private final MessageRepository messageRepository;
  private final RunService runService;
  private final ObjectMapper objectMapper;

  /**
   * Processes a message request by creating a user message and triggering a run.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the created run DTO
   */
  @Transactional
  public RunDto processMessage(UUID threadId, MessageCreateRequestDto request) {
    log.info("Processing message for thread: {}", threadId);

    // Create and persist user message
    createUserMessage(threadId, request);

    // Create run request using record constructor
    final var runRequest = new CreateRunRequestDto(threadId, null, request);

    // Trigger run execution
    final var runDto = runService.createRun(runRequest);

    log.info("Message processed and run created: {} for thread: {}", runDto.id(), threadId);
    return runDto;
  }

  /**
   * Creates a user message entity.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   */
  private void createUserMessage(UUID threadId, MessageCreateRequestDto request) {
    // Convert AuthorType to MessageEntity.Author
    final var author = MessageEntity.Author.valueOf(request.author().name());

    // Convert content to JsonNode
    final var contentJson = objectMapper.valueToTree(request.content());
    final var attachmentsJson = request.attachments() != null ?
        objectMapper.valueToTree(request.attachments()) : null;

    final var message = MessageEntity.builder()
        .id(UUID.randomUUID())
        .threadId(threadId)
        .author(author)
        .contentJson(contentJson)
        .attachmentsJson(attachmentsJson)
        .createdAt(Instant.now())
        .build();

    messageRepository.save(message);
  }
}