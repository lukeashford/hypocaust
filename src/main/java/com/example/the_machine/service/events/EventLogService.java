package com.example.the_machine.service.events;

import com.example.the_machine.common.IdGenerator;
import com.example.the_machine.domain.EventLogEntity;
import com.example.the_machine.domain.EventType;
import com.example.the_machine.repo.EventLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventLogService {

  private final EventLogRepository eventLogRepository;
  private final IdGenerator idGenerator;

  @Transactional
  public UUID append(
      UUID threadId,
      UUID runId,
      UUID messageId,
      EventType eventType,
      JsonNode payloadJson,
      String dedupeKey
  ) {
    val entity = EventLogEntity.builder()
        .id(idGenerator.newId())
        .threadId(threadId)
        .runId(runId)
        .messageId(messageId)
        .eventType(eventType)
        .payload(payloadJson)
        .occurredAt(Instant.now())
        .dedupeKey(dedupeKey)
        .build();

    val saved = eventLogRepository.save(entity);
    return saved.getId();
  }
}