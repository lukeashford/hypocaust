package com.example.hypocaust.domain.event;

import com.example.hypocaust.dto.ArtifactDto;
import java.util.UUID;

public final class ArtifactUpdatedEvent extends ArtifactEvent<ArtifactDto> {

  public ArtifactUpdatedEvent(UUID taskExecutionId, ArtifactDto payload) {
    super(taskExecutionId, payload);
  }

  @Override
  public EventType type() {
    return EventType.ARTIFACT_UPDATED;
  }
}
