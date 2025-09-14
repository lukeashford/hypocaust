package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.dto.MessageIncomingDto;
import com.example.the_machine.dto.MessageOutgoingDto;
import com.example.the_machine.service.MessageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling message processing requests.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class MessageController {

  private final MessageService messageService;

  /**
   * Processes a message and creates a run.
   *
   * @param threadId the thread ID
   * @param request the message creation request
   * @return the created run DTO
   */
  @PostMapping(Routes.THREAD_MESSAGES)
  public ResponseEntity<MessageOutgoingDto> processMessage(
      @PathVariable UUID threadId,
      @RequestBody MessageIncomingDto request) {

    log.info("Processing message for thread: {}", threadId);

    final var run = messageService.processMessage(threadId, request);
    return ResponseEntity.accepted().body(run);
  }
}