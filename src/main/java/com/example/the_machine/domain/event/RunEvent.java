package com.example.the_machine.domain.event;

import com.example.the_machine.domain.event.RunEvent.RunEventPayload;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract sealed class RunEvent<T extends RunEventPayload> extends Event<T>
    permits RunScheduledEvent, RunStartedEvent, RunCompletedEvent {

  protected RunEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface RunEventPayload extends EventPayload {

    UUID runId();
  }
}
