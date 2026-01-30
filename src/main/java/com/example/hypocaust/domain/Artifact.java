package com.example.hypocaust.domain;

import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventPayload;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

/**
 * Domain representation of an artifact, used both in memory during task execution and as a DTO for
 * the frontend.
 */
@Builder
public record Artifact(
    String fileName,
    ArtifactKind kind,
    String url,
    JsonNode content,
    String title,
    String description,
    ArtifactStatus status,
    JsonNode metadata
) implements ArtifactEventPayload {

}
