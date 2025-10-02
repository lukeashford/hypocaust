package com.example.the_machine.domain.event;

import com.example.the_machine.db.ArtifactEntity.Kind;
import com.example.the_machine.domain.event.ArtifactCancelledEvent.ArtifactCancelledEventPayload;
import java.util.UUID;

public final class ArtifactCancelledEvent extends ArtifactEvent<ArtifactCancelledEventPayload> {

  public ArtifactCancelledEvent(UUID threadId, UUID artifactId, Kind kind) {
    super(threadId, new ArtifactCancelledEventPayload(artifactId, kind));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_CANCELLED;
  }

  public record ArtifactCancelledEventPayload(
      UUID artifactId,
      Kind kind
  ) implements ArtifactEventEventPayload {

  }
}
