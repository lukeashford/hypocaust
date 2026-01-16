package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ArtifactCreatedEvent.ArtifactCreatedEventPayload;
import java.util.UUID;

public final class ArtifactCreatedEvent extends ArtifactEvent<ArtifactCreatedEventPayload> {

  public ArtifactCreatedEvent(UUID projectId, UUID artifactId) {
    super(projectId, new ArtifactCreatedEventPayload(artifactId));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_CREATED;
  }

  public record ArtifactCreatedEventPayload(
      UUID artifactId
  ) implements ArtifactEventEventPayload {

  }
}
