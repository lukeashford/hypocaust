package com.example.the_machine.domain.event;

import com.example.the_machine.db.ArtifactEntity.Kind;
import com.example.the_machine.domain.event.ArtifactEvent.ArtifactEventEventPayload;
import java.util.UUID;

public abstract sealed class ArtifactEvent<T extends ArtifactEventEventPayload> extends Event<T>
    permits ArtifactScheduledEvent, ArtifactCreatedEvent, ArtifactCancelledEvent {

  protected ArtifactEvent(UUID threadId, T payload) {
    super(threadId, payload);
  }

  public interface ArtifactEventEventPayload extends EventPayload {

    UUID artifactId();

    Kind kind();
  }
}
