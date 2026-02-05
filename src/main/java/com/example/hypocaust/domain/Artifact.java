package com.example.hypocaust.domain;

import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventPayload;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;

/**
 * Domain representation of an artifact, used both in memory during task execution and as a DTO for
 * the frontend.
 *
 * @param name project-unique identifier
 * @param kind type of artifact
 * @param url URL of the manifested artifact
 * @param inlineContent inline content (for text)
 * @param title human-readable title
 * @param description human-readable description
 * @param status current status
 * @param prompt prompt used to generate this artifact
 * @param model model used to generate this artifact
 * @param metadata size, duration, etc.
 */
@Builder
public record Artifact(
    @NonNull String name,
    @NonNull ArtifactKind kind,
    String url,
    JsonNode inlineContent,
    @NonNull String title,
    @NonNull String description,
    @NonNull ArtifactStatus status,
    String prompt,
    String model,
    JsonNode metadata
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
        .prompt(draft.prompt())
        .model(draft.model())
        .metadata(draft.metadata())
        .build();
  }

  public Artifact withStatus(ArtifactStatus status) {
    return new Artifact(name, kind, url, inlineContent, title, description, status, prompt, model,
        metadata);
  }

  public Artifact withUrl(String url) {
    return new Artifact(name, kind, url, inlineContent, title, description, status, prompt, model,
        metadata);
  }
}
