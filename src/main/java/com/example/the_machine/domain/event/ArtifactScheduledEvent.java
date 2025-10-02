package com.example.the_machine.domain.event;

import com.example.the_machine.db.ArtifactEntity.Kind;
import com.example.the_machine.domain.event.ArtifactScheduledEvent.ArtifactScheduledEventPayload;
import java.util.UUID;

public final class ArtifactScheduledEvent extends ArtifactEvent<ArtifactScheduledEventPayload> {

  public ArtifactScheduledEvent(UUID threadId, UUID artifactId, Kind kind) {
    super(threadId, new ArtifactScheduledEventPayload(artifactId, kind));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_SCHEDULED;
  }

  public record ArtifactScheduledEventPayload(
      UUID artifactId,
      Kind kind
  ) implements ArtifactEventEventPayload {

  }
}
