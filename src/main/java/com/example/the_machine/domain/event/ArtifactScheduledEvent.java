package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.ArtifactScheduledEvent.ArtifactScheduledEventPayload;
import java.util.UUID;

public final class ArtifactScheduledEvent extends ArtifactEvent<ArtifactScheduledEventPayload> {

  public ArtifactScheduledEvent(UUID projectId, UUID artifactId) {
    super(projectId, new ArtifactScheduledEventPayload(artifactId));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_SCHEDULED;
  }

  public record ArtifactScheduledEventPayload(
      UUID artifactId
  ) implements ArtifactEventEventPayload {

  }
}
