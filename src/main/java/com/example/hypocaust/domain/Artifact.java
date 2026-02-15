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
    @Schema(description = "Unique artifact ID")
    UUID id,

    @Schema(description = "Project-unique semantic name used to identify and deduplicate artifacts",
        example = "cat-astronaut-illustration")
    @NonNull String name,

    @Schema(description = "Content type category")
    @NonNull ArtifactKind kind,

    @Schema(description = "URL to fetch the resource from (available once status is MANIFESTED)",
        nullable = true)
    String url,

    @Schema(description = "Inline content (for TEXT kind). Display directly as text.",
        nullable = true)
    JsonNode inlineContent,

    @Schema(description = "Human-readable title", example = "Cat Astronaut Illustration")
    @NonNull String title,

    @Schema(description = "Human-readable description of what this artifact contains")
    @NonNull String description,

    @Schema(description = "Processing status. GESTATING = still generating (show skeleton), "
        + "MANIFESTED = ready to display")
    @NonNull ArtifactStatus status,

    @Schema(description = "Additional metadata (dimensions, file size, etc.)", nullable = true)
    JsonNode metadata,

    @Schema(description = "Technical MIME type", example = "image/webp")
    String mimeType
) implements ArtifactEventPayload {

  public static Artifact fromDraft(String name, ArtifactDraft draft) {
    return Artifact.builder()
        .name(name)
        .kind(draft.kind())
        .url(draft.url())
        .inlineContent(draft.inlineContent())
        .title(draft.title())
        .description(draft.description())
        .status(draft.status())
        .metadata(draft.metadata())
        .mimeType(null) // Drafts don't have mimeType yet
        .build();
  }

  public Artifact withStatus(ArtifactStatus status) {
    return new Artifact(id, name, kind, url, inlineContent, title, description, status, metadata,
        mimeType);
  }

  public Artifact withUrl(String url) {
    return new Artifact(id, name, kind, url, inlineContent, title, description, status, metadata,
        mimeType);
  }

  public Artifact withMimeType(String mimeType) {
    return new Artifact(id, name, kind, url, inlineContent, title, description, status, metadata,
        mimeType);
  }

  public Artifact withMetadata(JsonNode metadata) {
    return new Artifact(id, name, kind, url, inlineContent, title, description, status, metadata,
        mimeType);
  }

  public Artifact withInlineContent(JsonNode inlineContent) {
    return new Artifact(id, name, kind, url, inlineContent, title, description, status, metadata,
        mimeType);
  }
}
