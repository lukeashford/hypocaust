package com.example.the_machine.service.events;

import com.example.the_machine.dto.EventEnvelopeDto;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventPublisher {

  private final EventLogService eventLogService;
  private final SseHub sseHub;

  public void publish(EventEnvelopeDto e) {
    // For direct publish without persistence, just broadcast to live subscribers
    sseHub.broadcast(e.threadId(), null, e.type(), e.data());
  }

  public void publishAndStore(UUID threadId, EventEnvelopeDto e, String dedupeKeyOrNull) {
    val eventId = eventLogService.append(
        threadId, e.runId(), e.messageId(), e.type(), e.data(), dedupeKeyOrNull
    );
    sseHub.broadcast(threadId, eventId, e.type(), e.data());
  }
}