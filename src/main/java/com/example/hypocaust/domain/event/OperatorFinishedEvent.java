package com.example.hypocaust.domain.event;

import java.util.Map;
import java.util.UUID;

public final class OperatorFinishedEvent extends OperatorEvent<OperatorFinishedEvent.Payload> {

  public OperatorFinishedEvent(UUID taskExecutionId, String operatorName,
      Map<String, Object> inputs,
      Map<String, Object> outputs, String taskPath) {
    super(taskExecutionId, new Payload(operatorName, inputs, outputs, taskPath));
  }

  @Override
  public EventType type() {
    return EventType.OPERATOR_FINISHED;
  }

  public record Payload(String operatorName, Map<String, Object> inputs,
                        Map<String, Object> outputs, String taskPath) implements
      OperatorEventPayload {

  }
}
