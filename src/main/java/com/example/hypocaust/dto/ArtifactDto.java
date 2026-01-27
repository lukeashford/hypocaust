package com.example.hypocaust.dto;

import com.example.hypocaust.db.ArtifactEntity;

/**
 * Frontend-ready artifact view with pending status.
 *
 * @param name        Semantic artifact name
 * @param kind        Type of artifact (IMAGE, STRUCTURED_JSON, etc.)
 * @param description Human-readable description of the artifact
 * @param url         Resolved URL for frontend display
 * @param isPending   True if from pending changes, not yet persisted
 * @param status      SCHEDULED, CREATED, CANCELLED (for pending artifacts)
 */
public record ArtifactDto(
    String name,
    ArtifactEntity.Kind kind,
    String description,
    String url,
    boolean isPending,
    ArtifactEntity.Status status
) {

}
