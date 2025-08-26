package com.example.the_machine.dto;

import com.example.the_machine.domain.ArtifactEntity;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ArtifactDTO(
    UUID id,
    UUID threadId,
    UUID runId,
    ArtifactEntity.Kind kind,
    ArtifactEntity.Stage stage,
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