package com.example.hypocaust.domain.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public final class ArtifactUpdatedEvent extends ArtifactEvent<ArtifactUpdatedEvent.Payload> {

  public ArtifactUpdatedEvent(UUID projectId, String name, String description,
      String externalUrl, JsonNode inlineContent, JsonNode metadata) {
    super(projectId, new Payload(name, description, externalUrl, inlineContent, metadata));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_UPDATED;
  }

  public record Payload(
      String name,
      String description,
      String externalUrl,
      JsonNode inlineContent,
      JsonNode metadata
  ) implements ArtifactEventPayload {

  }
}
