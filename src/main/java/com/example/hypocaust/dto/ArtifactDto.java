package com.example.hypocaust.dto;

import com.example.hypocaust.db.ArtifactEntity;
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
    String subtitle,
    String alt,
    String mime,
    JsonNode metadata,           // dims, duration, etc.
    Instant createdAt,
    UUID supersededById
) {

}