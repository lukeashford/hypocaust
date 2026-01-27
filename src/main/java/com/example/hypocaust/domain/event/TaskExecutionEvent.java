package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.TaskExecutionEvent.TaskExecutionEventPayload;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract sealed class TaskExecutionEvent<T extends TaskExecutionEventPayload> extends Event<T>
    permits TaskExecutionStartedEvent, TaskExecutionCompletedEvent, TaskExecutionFailedEvent {

  protected TaskExecutionEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface TaskExecutionEventPayload extends EventPayload {

  }
}
