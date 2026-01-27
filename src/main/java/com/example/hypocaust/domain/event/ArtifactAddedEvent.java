package com.example.hypocaust.domain.event;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public final class ArtifactAddedEvent extends ArtifactEvent<ArtifactAddedEvent.Payload> {

  public ArtifactAddedEvent(UUID projectId, String name, Kind kind, String description,
      String externalUrl, JsonNode inlineContent, JsonNode metadata) {
    super(projectId, new Payload(name, kind, description, externalUrl, inlineContent, metadata));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_ADDED;
  }

  public record Payload(
      String name,
      Kind kind,
      String description,
      String externalUrl,
      JsonNode inlineContent,
      JsonNode metadata
  ) implements ArtifactEventPayload {

  }
}
