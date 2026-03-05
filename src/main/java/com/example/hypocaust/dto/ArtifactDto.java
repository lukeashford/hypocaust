package com.example.hypocaust.dto;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventPayload;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Frontend-facing representation of an Artifact. The internal storageKey is replaced with a
 * presigned URL for direct client access.
 */
@Schema(description = "A generated artifact (image, document, structured data, etc.)")
public record ArtifactDto(
    UUID id,
    String name,
    ArtifactKind kind,
    @Schema(description = "URL to fetch the resource from (available once status is MANIFESTED)",
        nullable = true)
    String url,
    @Schema(description = "Inline content (for TEXT kind). Display directly as text.",
        nullable = true, type = "string")
    JsonNode inlineContent,
    String title,
    String description,
    ArtifactStatus status,
    JsonNode metadata,
    String mimeType,
    String errorMessage
) implements ArtifactEventPayload {

  /**
   * Convert a domain Artifact to a frontend DTO, mapping storageKey to a presigned URL.
   */
  public static ArtifactDto from(Artifact artifact) {
    return new ArtifactDto(
        artifact.id(),
        artifact.name(),
        artifact.kind(),
        artifact.url(),
        artifact.inlineContent(),
        artifact.title(),
        artifact.description(),
        artifact.status(),
        artifact.metadata(),
        artifact.mimeType(),
        artifact.errorMessage()
    );
  }
}
