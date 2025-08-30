package com.example.the_machine.dto;

import com.example.the_machine.domain.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record EventEnvelopeDto(
    EventType type,
    UUID threadId,
    UUID runId,
    UUID messageId,
    JsonNode data
) {

}