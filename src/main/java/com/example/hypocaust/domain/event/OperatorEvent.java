package com.example.hypocaust.domain.event;

import com.example.hypocaust.domain.event.OperatorEvent.OperatorEventPayload;
import java.util.Map;
import java.util.UUID;

public abstract sealed class OperatorEvent<T extends OperatorEventPayload> extends Event<T>
    permits OperatorStartedEvent, OperatorFinishedEvent, OperatorFailedEvent {

  protected OperatorEvent(UUID projectId, T payload) {
    super(projectId, payload);
  }

  public interface OperatorEventPayload extends EventPayload {

    String operatorName();

    Map<String, Object> inputs();
  }
}
