package com.example.hypocaust.domain.event;

import java.util.UUID;

public final class ArtifactRemovedEvent extends ArtifactEvent<ArtifactRemovedEvent.Payload> {

  public ArtifactRemovedEvent(UUID projectId, String name) {
    super(projectId, new Payload(name));
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_REMOVED;
  }

  public record Payload(String name) implements ArtifactEventPayload {

  }
}
