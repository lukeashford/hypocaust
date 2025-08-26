package com.example.the_machine.service;

import com.example.the_machine.domain.MessageEntity;
import com.example.the_machine.dto.CreateRunRequest;
import com.example.the_machine.dto.MessageCreateRequest;
import com.example.the_machine.dto.RunDTO;
import com.example.the_machine.repo.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
  public RunDTO processMessage(UUID threadId, MessageCreateRequest request) {
    log.info("Processing message for thread: {}", threadId);

    // Create and persist user message
    createUserMessage(threadId, request);

    // Create run request using record constructor
    val runRequest = new CreateRunRequest(threadId, null, request);

    // Trigger run execution
    val runDto = runService.createRun(runRequest);

    log.info("Message processed and run created: {} for thread: {}", runDto.id(), threadId);
    return runDto;
  }

  /**
   * Creates a user message entity.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the created message entity
   */
  private MessageEntity createUserMessage(UUID threadId, MessageCreateRequest request) {
    // Convert AuthorType to MessageEntity.Author
    val author = MessageEntity.Author.valueOf(request.author().name());

    // Convert content to JsonNode
    val contentJson = objectMapper.valueToTree(request.content());
    val attachmentsJson = request.attachments() != null ?
        objectMapper.valueToTree(request.attachments()) : null;

    val message = MessageEntity.builder()
        .id(UUID.randomUUID())
        .threadId(threadId)
        .author(author)
        .contentJson(contentJson)
        .attachmentsJson(attachmentsJson)
        .createdAt(Instant.now())
        .build();

    return messageRepository.save(message);
  }
}