package com.example.the_machine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record RunDto(
    UUID id,
    UUID threadId,
    UUID assistantId,
    RunStatus status,
    RunKind kind,
    String reason,
    Instant startedAt,
    Instant completedAt,
    JsonNode usage,
    String error
) {

}