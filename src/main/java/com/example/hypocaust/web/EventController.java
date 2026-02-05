package com.example.hypocaust.web;

import com.example.hypocaust.common.Routes;
import com.example.hypocaust.service.events.EventService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@Slf4j
public class EventController {

  private final EventService eventService;

  /**
   * Subscribe to SSE events for a TaskExecution. This is the new endpoint per the concept
   * document.
   */
  @GetMapping(value = Routes.TASK_EXECUTION_EVENTS)
  public SseEmitter getTaskExecutionEvents(
      @PathVariable UUID taskExecutionId,
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {

    log.debug("SSE subscription request for TaskExecution {} with lastEventId: {}", taskExecutionId,
        lastEventId);
    return eventService.subscribeToTaskExecutionEvents(taskExecutionId, lastEventId);
  }

}