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

  @GetMapping(value = Routes.PROJECT_EVENTS)
  public SseEmitter getEvents(
      @PathVariable UUID id,
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {

    log.debug("SSE subscription request for project {} with lastEventId: {}", id, lastEventId);
    return eventService.subscribeToEvents(id, lastEventId);
  }

  @GetMapping(value = Routes.PROJECT_LOGS, produces = "text/plain")
  public String getProjectLogs(@PathVariable UUID id) {
    log.debug("Fetching logs for project {}", id);
    return eventService.getProjectLogs(id);
  }
}