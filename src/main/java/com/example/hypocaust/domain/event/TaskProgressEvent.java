package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.TaskProgressEvent.TaskProgressEventPayload;
import java.util.UUID;

public abstract sealed class TaskProgressEvent<T extends TaskProgressEventPayload> extends Event<T>
    permits TodoListUpdatedEvent {

  protected TaskProgressEvent(UUID taskExecutionId, T payload) {
    super(taskExecutionId, payload);
  }

  public interface TaskProgressEventPayload extends EventPayload {

  }
}
