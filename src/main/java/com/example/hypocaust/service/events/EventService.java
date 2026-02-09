package com.example.hypocaust.service.events;

import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.mapper.EventMapper;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.repo.EventLogRepository;
import com.example.hypocaust.repo.TaskExecutionRepository;
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
  private final TaskExecutionRepository taskExecutionRepository;

  @Transactional
  public void publish(Event<?> event, boolean doPersist) {
    log.debug("Publishing event: {}", event);
    final var entity = eventMapper.toEntity(event);

    UUID executionId = event.getTaskExecutionId();
    if (executionId == null && TaskExecutionContextHolder.hasContext()) {
      executionId = TaskExecutionContextHolder.getTaskExecutionId();
    }

    if (executionId != null) {
      entity.setTaskExecutionId(executionId);
    }

    if (doPersist) {
      eventLogRepository.save(entity);
    }

    if (executionId != null) {
      TaskExecutionContextHolder.getContextByTaskExecutionId(
              executionId)
          .ifPresent(ctx -> ctx.updateLastEventId(entity.getId()));
    }

    sseHub.broadcast(executionId, entity.getId(), event);
  }

  @Transactional
  public void publish(Event<?> event) {
    publish(event, true);
  }

  /**
   * Subscribe to SSE events for a TaskExecution.
   */
  public SseEmitter subscribeToTaskExecutionEvents(UUID taskExecutionId, UUID lastEventId) {
    if (!taskExecutionRepository.existsById(taskExecutionId)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "TaskExecution not found: " + taskExecutionId);
    }

    final var replayEvents = findEventsForExecutionSince(taskExecutionId, lastEventId);

    log.debug("SSE subscription for TaskExecution {} with lastEventId: {}",
        taskExecutionId, lastEventId);
    return sseHub.subscribe(taskExecutionId, replayEvents);
  }

  private List<SseHub.ReplayItem> findEventsForExecutionSince(UUID taskExecutionId,
      UUID lastEventId) {
    if (lastEventId == null) {
      // No lastEventId means this is a new connection - replay all events for this execution
      return eventLogRepository.findByTaskExecutionIdOrderById(taskExecutionId)
          .parallelStream()
          .map(entity -> new SseHub.ReplayItem(entity.getId(), eventMapper.toDomain(entity)))
          .collect(Collectors.toUnmodifiableList());
    }

    if (!eventLogRepository.existsByIdAndTaskExecutionId(lastEventId, taskExecutionId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Last-Event-ID not found for execution: " + lastEventId);
    }

    return eventLogRepository.findByTaskExecutionIdAndIdGreaterThanOrderById(taskExecutionId,
            lastEventId)
        .parallelStream()
        .map(entity -> new SseHub.ReplayItem(entity.getId(), eventMapper.toDomain(entity)))
        .collect(Collectors.toUnmodifiableList());
  }
}