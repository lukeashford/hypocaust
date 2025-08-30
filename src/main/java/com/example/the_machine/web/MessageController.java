package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.dto.MessageCreateRequestDto;
import com.example.the_machine.dto.RunDto;
import com.example.the_machine.service.MessageService;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
  public ResponseEntity<RunDto> processMessage(
      @PathVariable UUID threadId,
      @RequestBody MessageCreateRequestDto request) {

    log.info("Processing message for thread: {}", threadId);

    try {
      val run = messageService.processMessage(threadId, request);
      return ResponseEntity.ok(run);
    } catch (RejectedExecutionException e) {
      log.warn("Execution rejected for thread {}: {}", threadId, e.getMessage());
      return ResponseEntity.status(429).build(); // Too Many Requests
    } catch (IllegalArgumentException e) {
      log.warn("Invalid request for thread: {} - {}", threadId, e.getMessage());
      return ResponseEntity.badRequest().build();
    } catch (Exception e) {
      log.error("Error processing message for thread: {}", threadId, e);
      return ResponseEntity.internalServerError().build();
    }
  }
}