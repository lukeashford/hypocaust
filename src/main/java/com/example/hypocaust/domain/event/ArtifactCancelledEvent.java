package com.example.hypocaust.domain.event;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.domain.event.ArtifactCancelledEvent.ArtifactCancelledEventPayload;
import java.util.UUID;

public final class ArtifactCancelledEvent extends ArtifactEvent<ArtifactCancelledEventPayload> {

  public ArtifactCancelledEvent(UUID projectId, UUID artifactId, Kind kind) {
    super(projectId, new ArtifactCancelledEventPayload(artifactId, kind));
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
