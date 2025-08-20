package com.example.the_machine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ArtifactDTO(
    UUID id,
    UUID threadId,
    UUID runId,
    ArtifactKind kind,
    ArtifactStage stage,
    ArtifactStatus status,
    String title,
    String summary,
    String mime,
    String url,                  // computed if file-backed
    JsonNode inlineJson,         // structured outputs
    JsonNode meta,               // dims, duration, etc.
    Instant createdAt,
    UUID supersedesId
) {

}