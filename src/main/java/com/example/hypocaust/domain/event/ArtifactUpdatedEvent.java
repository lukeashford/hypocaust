package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.ArtifactEvent.ArtifactEventPayload;
import java.util.UUID;

public final class ArtifactUpdatedEvent extends ArtifactEvent<ArtifactEventPayload> {

  public ArtifactUpdatedEvent(UUID taskExecutionId, ArtifactEventPayload payload) {
    super(taskExecutionId, payload);
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_UPDATED;
  }
}
