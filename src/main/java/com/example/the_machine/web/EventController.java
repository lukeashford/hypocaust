package com.example.the_machine.web;

import com.example.the_machine.common.Routes;
import com.example.the_machine.service.events.EventService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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

  @GetMapping(value = Routes.THREAD_EVENTS, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter getEvents(
      @PathVariable UUID id,
      @RequestHeader(value = "Last-Event-ID", required = false) UUID lastEventId) {

    log.debug("SSE subscription request for thread {} with lastEventId: {}", id, lastEventId);
    return eventService.subscribeToEvents(id, lastEventId);
  }
}