package com.example.hypocaust.domain.event;

import java.util.List;
import java.util.UUID;

public final class DecomposerFinishedEvent
    extends DecomposerEvent<DecomposerFinishedEvent.Payload> {

  public DecomposerFinishedEvent(UUID taskExecutionId, String task, String summary,
      List<String> artifactNames) {
    super(taskExecutionId, new Payload(task, summary, artifactNames));
  }

  @Override
  public EventType type() {
    return EventType.DECOMPOSER_FINISHED;
  }

  public record Payload(String task, String summary, List<String> artifactNames)
      implements DecomposerEventPayload {

  }
}
