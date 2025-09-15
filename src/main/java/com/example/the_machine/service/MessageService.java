package com.example.the_machine.service;

import com.example.the_machine.db.MessageEntity;
import com.example.the_machine.db.MessageEntity.Author;
import com.example.the_machine.domain.event.MessageProcessingEvent;
import com.example.the_machine.dto.MessageIncomingDto;
import com.example.the_machine.dto.MessageOutgoingDto;
import com.example.the_machine.mapper.MessageMapper;
import com.example.the_machine.repo.MessageRepository;
import com.example.the_machine.service.events.EventService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
   * @param request the message creation requestk
   * @return the chat response DTO with immediate status
   */
  public MessageOutgoingDto processMessage(UUID threadId, MessageIncomingDto request) {
    log.info("Processing chat message for thread: {}", threadId);

    // Create and persist user message immediately
    final var userMessage = MessageEntity.builder()
        .threadId(threadId)
        .author(Author.USER)
        .content(request.content())
        .build();
    messageRepository.save(userMessage);

    // Process asynchronously
    centralChatService.processChatMessage(threadId, userMessage.getId());

    eventService.publish(new MessageProcessingEvent(threadId, userMessage.getId()));

    // Return immediate response
    return messageMapper.toDto(userMessage);
  }
}