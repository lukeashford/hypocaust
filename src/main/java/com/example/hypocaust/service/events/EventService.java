package com.example.hypocaust.service.events;

import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.mapper.EventMapper;
import com.example.hypocaust.repo.EventLogRepository;
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
  private final EventLogRepository eventLogRepository;
  private final EventMapper eventMapper;

  @Transactional
  public void publish(Event<?> event, boolean doPersist) {
    log.debug("Publishing event: {}", event);
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

  public SseEmitter subscribeToEvents(UUID projectId, UUID lastEventId) {
    final var replayEvents = findEventsSince(projectId, lastEventId);

    log.debug("SSE subscription for project {} with lastEventId: {}", projectId, lastEventId);
    return sseHub.subscribe(projectId, replayEvents);
  }

  private List<Event<?>> findEventsSince(UUID projectId, UUID lastEventId) {
    if (lastEventId == null) {
      // No lastEventId means this is a new connection - replay all events
      return eventLogRepository.findByProjectIdOrderById(projectId)
          .parallelStream()
          .map(eventMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }

    if (!eventLogRepository.existsByIdAndProjectId(lastEventId, projectId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Last-Event-ID not found for project: " + lastEventId);
    }

    return eventLogRepository.findByProjectIdAndIdGreaterThanOrderById(projectId, lastEventId)
        .parallelStream()
        .map(eventMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}