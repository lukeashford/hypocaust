package com.example.hypocaust.domain;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.db.ArtifactEntity.Status;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

/**
 * Represents a pending artifact that hasn't been persisted yet.
 * Used by TaskExecutionContext to accumulate changes during execution.
 *
 * @param name          Generated artifact name (set after addArtifact returns)
 * @param kind          The type of artifact (IMAGE, STRUCTURED_JSON, etc.)
 * @param description   Human-readable description of the artifact (required)
 * @param prompt        The prompt used to generate it
 * @param model         The model used for generation
 * @param externalUrl   URL to download from (for images)
 * @param inlineContent For structured content
 * @param metadata      Additional technical metadata
 * @param status        Current status (SCHEDULED, CREATED, CANCELLED)
 */
@Builder
public record PendingArtifact(
    String name,
    Kind kind,
    String description,
    String prompt,
    String model,
    String externalUrl,
    JsonNode inlineContent,
    JsonNode metadata,
    Status status
) {

  /**
   * Creates a copy with the given name.
   */
  public PendingArtifact withName(String newName) {
    return new PendingArtifact(newName, kind, description, prompt, model, externalUrl, inlineContent, metadata, status);
  }

  /**
   * Creates a copy with the given status.
   */
  public PendingArtifact withStatus(Status newStatus) {
    return new PendingArtifact(name, kind, description, prompt, model, externalUrl, inlineContent, metadata, newStatus);
  }
}
