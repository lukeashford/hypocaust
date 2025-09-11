package com.example.the_machine.web

import com.example.the_machine.common.Routes
import com.example.the_machine.service.events.EventService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.*

@RestController
class EventController(
  private val eventService: EventService
) {

  private val log = LoggerFactory.getLogger(EventController::class.java)

  @GetMapping(value = [Routes.THREAD_EVENTS], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
  fun getEvents(
    @PathVariable id: UUID,
    @RequestHeader(value = "Last-Event-ID", required = false) lastEventId: String?
  ): SseEmitter {
    log.debug("SSE subscription request for thread {} with lastEventId: {}", id, lastEventId)
    return eventService.subscribeToEvents(id, lastEventId)
  }
}