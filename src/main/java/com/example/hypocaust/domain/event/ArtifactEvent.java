package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventEventPayload;
import java.util.UUID;

public abstract sealed class ArtifactEvent<T extends ArtifactEventEventPayload> extends Event<T>
    permits ArtifactScheduledEvent, ArtifactCreatedEvent, ArtifactCancelledEvent {

  protected ArtifactEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface ArtifactEventEventPayload extends EventPayload {

    UUID artifactId();

  }
}
