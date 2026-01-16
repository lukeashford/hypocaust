package com.example.hypocaust.dto;

import com.example.hypocaust.db.ArtifactEntity;
import java.time.Instant;
import java.util.UUID;

public record ArtifactMetadataDto(
    UUID id,
    UUID projectId,
    UUID runId,
    ArtifactEntity.Kind kind,
    ArtifactEntity.Status status,
    String title,
    String mime,
    Instant createdAt
) {

}
