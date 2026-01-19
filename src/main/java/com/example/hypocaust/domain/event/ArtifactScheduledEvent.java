package com.example.hypocaust.domain.event;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import com.example.hypocaust.domain.event.ArtifactScheduledEvent.ArtifactScheduledEventPayload;
import java.util.UUID;

public final class ArtifactScheduledEvent extends ArtifactEvent<ArtifactScheduledEventPayload> {

  public ArtifactScheduledEvent(
      UUID projectId,
      UUID artifactId,
      Kind kind,
      String title,
      String subtitle,
      String alt
  ) {
    super(projectId, new ArtifactScheduledEventPayload(artifactId, kind, title, subtitle, alt));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_SCHEDULED;
  }

  public record ArtifactScheduledEventPayload(
      UUID artifactId,
      Kind kind,
      String title,
      String subtitle,
      String alt
  ) implements ArtifactEventEventPayload {

  }
}
