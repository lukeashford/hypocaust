package com.example.hypocaust.domain;

import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventPayload;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;

@Builder
@Schema(description = "A generated artifact (image, document, structured data, etc.)")
public record Artifact(
    @Schema(description = "ID of the artifact version (null if not yet persisted)",
        nullable = true)
    UUID id,

    @Schema(description = "Project-unique semantic name used to identify and deduplicate artifacts",
        example = "cat-astronaut-illustration",
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull String name,

    @Schema(description = "Content type category", requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull ArtifactKind kind,

    @Schema(description = "Storage key for file-based artifacts (internal). "
        + "Mapped to a presigned URL at API boundaries.",
        nullable = true)
    String storageKey,

    @Schema(description = "Inline content (for TEXT kind). Display directly as text.",
        nullable = true, type = "string")
    JsonNode inlineContent,

    @Schema(description = "Human-readable title", example = "Cat Astronaut Illustration", requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull String title,

    @Schema(description = "Human-readable description of what this artifact contains", requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull String description,

    @Schema(description = "Processing status. GESTATING = still generating (show skeleton), "
        + "MANIFESTED = ready to display", requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull ArtifactStatus status,

    @Schema(description = "Additional metadata (dimensions, file size, etc.)", nullable = true)
    JsonNode metadata,

    @Schema(description = "Technical MIME type", example = "image/webp")
    String mimeType,

    @Schema(description = "Error message describing why generation or storage failed", nullable = true)
    String errorMessage
) implements ArtifactEventPayload {

  public static Artifact fromDraft(String name, ArtifactDraft draft) {
    return Artifact.builder()
        .id(null)
        .name(name)
        .kind(draft.kind())
        .inlineContent(draft.inlineContent())
        .title(draft.title())
        .description(draft.description())
        .status(draft.status())
        .metadata(draft.metadata())
        .build();
  }

  public Artifact withStatus(ArtifactStatus status) {
    return new Artifact(id, name, kind, storageKey, inlineContent, title, description, status,
        metadata, mimeType, errorMessage);
  }

  public Artifact withStorageKey(String storageKey) {
    return new Artifact(id, name, kind, storageKey, inlineContent, title, description, status,
        metadata, mimeType, errorMessage);
  }

  public Artifact withMimeType(String mimeType) {
    return new Artifact(id, name, kind, storageKey, inlineContent, title, description, status,
        metadata, mimeType, errorMessage);
  }

  public Artifact withMetadata(JsonNode metadata) {
    return new Artifact(id, name, kind, storageKey, inlineContent, title, description, status,
        metadata, mimeType, errorMessage);
  }

  public Artifact withInlineContent(JsonNode inlineContent) {
    return new Artifact(id, name, kind, storageKey, inlineContent, title, description, status,
        metadata, mimeType, errorMessage);
  }

  public Artifact withErrorMessage(String errorMessage) {
    return new Artifact(id, name, kind, storageKey, inlineContent, title, description, status,
        metadata, mimeType, errorMessage);
  }
}
