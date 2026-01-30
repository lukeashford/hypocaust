package com.example.hypocaust.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskExecutionDto(
    UUID id,
    UUID projectId,
    String task,
    RunStatus status,
    String reason,
    Instant startedAt,
    Instant completedAt
) {

}