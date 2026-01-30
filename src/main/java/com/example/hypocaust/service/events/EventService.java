package com.example.hypocaust.service.events;

import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.mapper.EventMapper;
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

    UUID executionId = null;
    if (com.example.hypocaust.operator.TaskExecutionContextHolder.hasContext()) {
      executionId = com.example.hypocaust.operator.TaskExecutionContextHolder.getTaskExecutionId();
      entity.setTaskExecutionId(executionId);
    }

    if (doPersist) {
      eventLogRepository.save(entity);
    }
    sseHub.broadcast(executionId, event);
  }

  @Transactional
  public void publish(Event<?> event) {
    publish(event, true);
  }

  public SseEmitter subscribeToEvents(UUID projectId, UUID lastEventId) {
    var latestExecutionId = taskExecutionRepository.findTopByProjectIdOrderByStartedAtDesc(
            projectId)
        .map(com.example.hypocaust.db.TaskExecutionEntity::getId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No executions found for project: " + projectId));

    log.debug("SSE legacy subscription for project {} redirected to latest execution {}", projectId,
        latestExecutionId);
    return subscribeToTaskExecutionEvents(latestExecutionId, lastEventId);
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

  public String getProjectLogs(UUID projectId) {
    log.debug("Fetching event history for project {}", projectId);
    return eventLogRepository.findByTaskExecutionIdOrderById(projectId)
        .stream()
        .map(eventMapper::toDomain)
        .map(this::formatEvent)
        .collect(Collectors.joining("\n"));
  }

  private String formatEvent(Event<?> event) {
    return String.format("[%s] %s: %s",
        event.getOccurredAt(),
        event.getType().getValue(),
        event.getPayload());
  }

  private List<Event<?>> findEventsForExecutionSince(UUID taskExecutionId, UUID lastEventId) {
    if (lastEventId == null) {
      // No lastEventId means this is a new connection - replay all events for this execution
      return eventLogRepository.findByTaskExecutionIdOrderById(taskExecutionId)
          .parallelStream()
          .map(eventMapper::toDomain)
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
        .map(eventMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}