package com.example.hypocaust.domain.event;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.domain.event.ArtifactCreatedEvent.ArtifactCreatedEventPayload;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public final class ArtifactCreatedEvent extends ArtifactEvent<ArtifactCreatedEventPayload> {

  public ArtifactCreatedEvent(
      UUID projectId,
      UUID artifactId,
      Kind kind,
      String title,
      String subtitle,
      String alt,
      String storageKey,
      JsonNode content
  ) {
    super(projectId, new ArtifactCreatedEventPayload(
        artifactId, kind, title, subtitle, alt, storageKey, content));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_CREATED;
  }

  public record ArtifactCreatedEventPayload(
      UUID artifactId,
      Kind kind,
      String title,
      String subtitle,
      String alt,
      String storageKey,
      JsonNode content
  ) implements ArtifactEventEventPayload {

  }
}
