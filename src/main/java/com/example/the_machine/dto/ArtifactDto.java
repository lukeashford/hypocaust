package com.example.the_machine.dto;

import com.example.the_machine.db.ArtifactEntity;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ArtifactDto(
    UUID id,
    UUID projectId,
    UUID runId,
    ArtifactEntity.Kind kind,
    ArtifactEntity.Status status,
    String title,
    String mime,
    String url,                  // computed if file-backed
    JsonNode content,            // structured outputs
    JsonNode metadata,           // dims, duration, etc.
    Instant createdAt,
    UUID supersededById
) {

}