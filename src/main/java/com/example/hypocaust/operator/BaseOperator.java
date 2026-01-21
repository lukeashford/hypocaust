package com.example.hypocaust.operator;

import com.example.hypocaust.domain.event.OperatorFailedEvent;
import com.example.hypocaust.domain.event.OperatorFinishedEvent;
import com.example.hypocaust.domain.event.OperatorStartedEvent;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.events.EventService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseOperator implements Operator {

  @Autowired
  protected EventService eventService;

  public final OperatorResult execute(Map<String, Object> rawInputs) {
    final var projectId = RunContextHolder.getProjectId();
    final var operatorName = getName();

    try {
      // First normalize (apply defaults), then validate
      final var normalizedInputs = spec().normalize(rawInputs);

      eventService.publish(new OperatorStartedEvent(projectId, operatorName, normalizedInputs));

      final var validationResult = spec().validate(normalizedInputs);
      if (!validationResult.ok()) {
        final var failure = OperatorResult.failure(validationResult.message(), normalizedInputs);
        eventService.publish(
            new OperatorFailedEvent(projectId, operatorName, normalizedInputs, failure.message()));
        return failure;
      }

      final var result = doExecute(normalizedInputs);

      if (result.ok()) {
        eventService.publish(
            new OperatorFinishedEvent(projectId, operatorName, normalizedInputs, result.outputs()));
      } else {
        eventService.publish(
            new OperatorFailedEvent(projectId, operatorName, normalizedInputs, result.message()));
      }

      return result;
    } catch (Exception e) {
      eventService.publish(
          new OperatorFailedEvent(projectId, operatorName, rawInputs, e.getMessage()));
      return OperatorResult.failure(e.getMessage(), rawInputs);
    }
  }

  public String getName() {
    return spec().name();
  }

  protected abstract OperatorResult doExecute(Map<String, Object> normalizedInputs);
}
