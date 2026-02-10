package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.domain.TaskExecutionSnapshot;
import com.example.hypocaust.domain.event.Event;
import com.example.hypocaust.service.TaskExecutionService;
import com.example.hypocaust.service.events.EventService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller for monitoring and interacting with task executions.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TaskExecutionController {

  private final TaskExecutionService taskExecutionService;
  private final EventService eventService;

  /**
   * Get the current state snapshot of a task execution.
   *
   * @param taskExecutionId the execution ID
   * @return snapshot of todos and context
   */
  @GetMapping(Routes.TASK_EXECUTION_STATE)
  public TaskExecutionSnapshot getTaskExecutionState(@PathVariable UUID taskExecutionId) {
    return taskExecutionService.getState(taskExecutionId);
  }

  /**
   * Get all events for a TaskExecution as a list.
   *
   * @param taskExecutionId the execution ID
   * @return list of events
   */
  @GetMapping(Routes.TASK_EXECUTION_EVENTS)
  public List<Event<?>> getTaskExecutionEventsList(@PathVariable UUID taskExecutionId) {
    return eventService.getEvents(taskExecutionId);
  }

  /**
   * Subscribe to SSE events for a TaskExecution.
   *
   * @param taskExecutionId the execution ID to subscribe to
   * @param lastEventId optional last event ID for replaying missed events
   * @return SSE emitter
   */
  @GetMapping(value = Routes.TASK_EXECUTION_EVENTS, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribeToTaskExecutionEvents(
      @PathVariable UUID taskExecutionId,
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {

    log.debug("SSE subscription request for TaskExecution {} with lastEventId: {}", taskExecutionId,
        lastEventId);
    return eventService.subscribeToTaskExecutionEvents(taskExecutionId, lastEventId);
  }
}
