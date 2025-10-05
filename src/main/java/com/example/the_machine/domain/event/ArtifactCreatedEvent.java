package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ArtifactCreatedEvent.ArtifactCreatedEventPayload;
import java.util.UUID;

public final class ArtifactCreatedEvent extends ArtifactEvent<ArtifactCreatedEventPayload> {

  public ArtifactCreatedEvent(UUID threadId, UUID artifactId) {
    super(threadId, new ArtifactCreatedEventPayload(artifactId));
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
