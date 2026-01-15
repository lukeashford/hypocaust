package com.example.the_machine.dto;

import com.example.the_machine.db.ArtifactEntity;
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
