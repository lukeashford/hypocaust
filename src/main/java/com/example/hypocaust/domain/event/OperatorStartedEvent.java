package com.example.hypocaust.domain.event;

import java.util.Map;
import java.util.UUID;

public final class OperatorStartedEvent extends OperatorEvent<OperatorStartedEvent.Payload> {

  public OperatorStartedEvent(UUID taskExecutionId, String operatorName, Map<String, Object> inputs,
      String taskPath) {
    super(taskExecutionId, new Payload(operatorName, inputs, taskPath));
  }

  @Override
  public EventType type() {
    return EventType.OPERATOR_STARTED;
  }

  public record Payload(String operatorName, Map<String, Object> inputs, String taskPath) implements
      OperatorEventPayload {

  }
}
