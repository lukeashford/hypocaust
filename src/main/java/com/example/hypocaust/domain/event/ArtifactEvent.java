package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventPayload;
import java.util.UUID;

public abstract sealed class ArtifactEvent<T extends ArtifactEventPayload> extends Event<T>
    permits ArtifactAddedEvent, ArtifactUpdatedEvent, ArtifactRemovedEvent {

  protected ArtifactEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface ArtifactEventPayload extends EventPayload {

    String name();
  }
}
