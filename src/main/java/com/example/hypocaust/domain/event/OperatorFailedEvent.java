package com.example.hypocaust.domain.event;

import java.util.Map;
import java.util.UUID;

public final class OperatorFailedEvent extends OperatorEvent<OperatorFailedEvent.Payload> {

  public OperatorFailedEvent(UUID taskExecutionId, String operatorName, Map<String, Object> inputs,
      String reason, String taskPath) {
    super(taskExecutionId, new Payload(operatorName, inputs, reason, taskPath));
  }

  @Override
  public EventType type() {
    return EventType.OPERATOR_FAILED;
  }

  public record Payload(String operatorName, Map<String, Object> inputs, String reason,
                        String taskPath) implements
      OperatorEventPayload {

  }
}
