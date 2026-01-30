package com.example.hypocaust.domain.event;

import com.example.hypocaust.dto.ArtifactDto;
import java.util.UUID;

public final class ArtifactAddedEvent extends ArtifactEvent<ArtifactDto> {

  public ArtifactAddedEvent(UUID taskExecutionId, ArtifactDto payload) {
    super(taskExecutionId, payload);
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_ADDED;
  }
}
