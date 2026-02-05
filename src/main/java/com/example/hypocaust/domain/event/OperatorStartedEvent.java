package com.example.hypocaust.domain.event;

import java.util.Map;
import java.util.UUID;

public final class OperatorStartedEvent extends OperatorEvent<OperatorStartedEvent.Payload> {

  public OperatorStartedEvent(UUID taskExecutionId, String operatorName,
      Map<String, Object> inputs) {
    super(taskExecutionId, new Payload(operatorName, inputs));
  }

  @Override
  public EventType type() {
    return EventType.OPERATOR_STARTED;
  }

  public record Payload(String operatorName, Map<String, Object> inputs) implements
      OperatorEventPayload {

  }
}
