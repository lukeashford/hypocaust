package com.example.the_machine.service.events;

import com.example.the_machine.repo.EventLogRepository;
import com.example.the_machine.repo.ThreadRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

  private final ThreadRepository threadRepository;
  private final EventLogRepository eventLogRepository;
  private final SseHub sseHub;

  public SseEmitter subscribeToEvents(UUID threadId, String lastEventIdFromHeader) {
    // 1. Validate thread exists
    threadRepository.findById(threadId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found: " + threadId)
    );

    // 2. Parse and validate lastEventId
    final var lastEventId = parseAndValidateLastEventId(lastEventIdFromHeader, threadId);

    // 3. Delegate to SseHub
    log.debug("SSE subscription for thread {} with validated lastEventId: {}", threadId,
        lastEventId);
    return sseHub.subscribe(threadId, lastEventId);
  }

  private UUID parseAndValidateLastEventId(String header, UUID threadId) {
    if (header == null || header.trim().isEmpty()) {
      return null;
    }

    UUID lastEventId;
    try {
      lastEventId = UUID.fromString(header.trim());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid Last-Event-ID format: " + header);
    }

    // Validate the event exists for this thread
    if (!eventLogRepository.existsByIdAndThreadId(lastEventId, threadId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Last-Event-ID not found for thread: " + lastEventId);
    }

    return lastEventId;
  }
}