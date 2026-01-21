package com.example.hypocaust.domain.event;

import java.util.Map;
import java.util.UUID;

public final class OperatorFailedEvent extends OperatorEvent<OperatorFailedEvent.Payload> {

  public OperatorFailedEvent(UUID projectId, String operatorName, Map<String, Object> inputs,
      String reason) {
    super(projectId, new Payload(operatorName, inputs, reason));
  }

  @Override
  public EventType type() {
    return EventType.OPERATOR_FAILED;
  }

  public record Payload(String operatorName, Map<String, Object> inputs, String reason) implements
      OperatorEventPayload {

  }
}
