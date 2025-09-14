package com.example.the_machine.service.events;

import com.example.the_machine.domain.event.Event;
import com.example.the_machine.mapper.EventMapper;
import com.example.the_machine.repo.EventLogRepository;
import com.example.the_machine.repo.ThreadRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

  private final SseHub sseHub;
  private final ThreadRepository threadRepository;
  private final EventLogRepository eventLogRepository;
  private final EventMapper eventMapper;

  @Transactional
  public void publish(Event<?> event, boolean doPersist) {
    final var entity = eventMapper.toEntity(event);
    if (doPersist) {
      eventLogRepository.save(entity);
    }
    sseHub.broadcast(event);
  }

  @Transactional
  public void publish(Event<?> event) {
    publish(event, true);
  }

  public SseEmitter subscribeToEvents(UUID threadId, UUID lastEventId) {
    // 1. Validate thread exists
    threadRepository.findById(threadId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found: " + threadId)
    );

    final var replayEvents = findEventsSince(threadId, lastEventId);

    // 3. Delegate to SseHub
    log.debug("SSE subscription for thread {} with validated lastEventId: {}", threadId,
        lastEventId);
    return sseHub.subscribe(threadId, replayEvents);
  }

  private List<Event<?>> findEventsSince(UUID threadId, UUID lastEventId) {
    if (lastEventId == null) {
      return null;
    }

    if (!eventLogRepository.existsByIdAndThreadId(lastEventId, threadId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Last-Event-ID not found for thread: " + lastEventId);
    }

    return eventLogRepository.findByThreadIdAndIdGreaterThanOrderById(threadId, lastEventId)
        .parallelStream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}