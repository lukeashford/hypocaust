package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.Artifact;
import java.util.UUID;

public final class ArtifactAddedEvent extends ArtifactEvent<Artifact> {

  public ArtifactAddedEvent(UUID taskExecutionId, Artifact payload) {
    super(taskExecutionId, payload);
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_ADDED;
  }
}
