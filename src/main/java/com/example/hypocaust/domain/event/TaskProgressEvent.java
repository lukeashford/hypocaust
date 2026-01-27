package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.TaskProgressEvent.TaskProgressEventPayload;
import java.util.UUID;

public abstract sealed class TaskProgressEvent<T extends TaskProgressEventPayload> extends Event<T>
    permits TaskProgressUpdatedEvent {

  protected TaskProgressEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface TaskProgressEventPayload extends EventPayload {

  }
}
