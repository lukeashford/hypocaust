package com.example.the_machine.service;

import com.example.the_machine.db.MessageEntity;
import com.example.the_machine.db.MessageEntity.Author;
import com.example.the_machine.domain.event.ErrorEvent;
import com.example.the_machine.domain.event.MessageProcessingEvent;
import com.example.the_machine.dto.MessageIncomingDto;
import com.example.the_machine.dto.MessageOutgoingDto;
import com.example.the_machine.mapper.MessageMapper;
import com.example.the_machine.repo.MessageRepository;
import com.example.the_machine.service.events.EventService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
  private final EventService eventService;
  private final MessageMapper messageMapper;
  private final CentralChatService centralChatService;

  /**
   * Processes a chat message request with immediate response and async LLM processing.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the chat response DTO with immediate status
   */
  @Transactional
  public MessageOutgoingDto processMessage(UUID threadId, MessageIncomingDto request) {
    log.info("Processing chat message for thread: {}", threadId);

    // Create and persist user message immediately
    final var userMessage = MessageEntity.builder()
        .threadId(threadId)
        .author(Author.USER)
        .content(request.message())
        .build();
    messageRepository.save(userMessage);

    // Process asynchronously
    CompletableFuture.runAsync(() -> processChatAsync(threadId, userMessage.getId()));

    // Return immediate response
    return messageMapper.toDto(userMessage);
  }

  /**
   * Processes chat message asynchronously with LLM and tool integration.
   *
   * @param threadId the thread ID
   * @param messageId the message ID
   */
  private void processChatAsync(UUID threadId, UUID messageId) {
    eventService.publish(new MessageProcessingEvent(threadId, messageId));

    try {
      // Process with LLM
      final var llmResponse = centralChatService.processChatMessage(threadId, messageId);

      log.info("LLM response for thread: {}, messageId: {}: {}", threadId, messageId, llmResponse);

    } catch (Exception e) {
      log.error("Error in async chat processing for thread: {}, messageId: {}", threadId, messageId,
          e);
      eventService.publish(
          new ErrorEvent(threadId, "Failed to process message: " + e.getMessage()));
    }
  }
}