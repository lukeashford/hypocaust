package com.example.hypocaust.operator;

import com.example.hypocaust.domain.event.OperatorFailedEvent;
import com.example.hypocaust.domain.event.OperatorFinishedEvent;
import com.example.hypocaust.domain.event.OperatorStartedEvent;
import com.example.hypocaust.exception.ArtifactExistsException;
import com.example.hypocaust.exception.ArtifactNotFoundException;
import com.example.hypocaust.exception.ArtifactTypeMismatchException;
import com.example.hypocaust.exception.ExternalServiceException;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.events.EventService;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for all operators. Provides consistent error handling and event emission.
 */
@Slf4j
public abstract class BaseOperator implements Operator {

  @Autowired
  protected EventService eventService;

  /**
   * Template method that wraps execution with error handling. Uses the default todoPath "0".
   */
  public final OperatorResult execute(Map<String, Object> rawInputs) {
    return execute(rawInputs, "0");
  }

  /**
   * Template method that wraps execution with error handling.
   *
   * @param rawInputs The raw inputs for the operator
   * @param todoPath The path identifying this operator's position in the task tree (e.g., "0.1.2")
   */
  public final OperatorResult execute(Map<String, Object> rawInputs, String todoPath) {
    final var taskExecutionId = TaskExecutionContextHolder.getTaskExecutionId();
    final var operatorName = getName();

    try {
      // First normalize (apply defaults), then validate
      final var normalizedInputs = spec().normalize(rawInputs);

      eventService.publish(
          new OperatorStartedEvent(taskExecutionId, operatorName, normalizedInputs, todoPath));

      final var validationResult = spec().validate(normalizedInputs);
      if (!validationResult.ok()) {
        final var failure = OperatorResult.failure(validationResult.message(), normalizedInputs);
        eventService.publish(
            new OperatorFailedEvent(taskExecutionId, operatorName, normalizedInputs,
                failure.message(), todoPath));
        return failure;
      }

      final var result = doExecute(normalizedInputs);

      if (result.ok()) {
        eventService.publish(
            new OperatorFinishedEvent(taskExecutionId, operatorName, normalizedInputs,
                result.outputs(), todoPath));
      } else {
        eventService.publish(
            new OperatorFailedEvent(taskExecutionId, operatorName, normalizedInputs,
                result.message(), todoPath));
      }

      return result;
    } catch (ArtifactNotFoundException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoPath,
          "Artifact not found: " + e.getMessage());
    } catch (ArtifactExistsException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoPath,
          "Artifact already exists: " + e.getArtifactName());
    } catch (ArtifactTypeMismatchException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoPath,
          "Type mismatch: " + e.getMessage());
    } catch (ExternalServiceException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoPath,
          "External service error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error in operator {}: {}", operatorName, e.getMessage(), e);
      return handleException(taskExecutionId, operatorName, rawInputs, todoPath,
          "Unexpected error: " + e.getMessage());
    }
  }

  private OperatorResult handleException(UUID taskExecutionId, String operatorName,
      Map<String, Object> inputs, String todoPath, String message) {
    eventService.publish(
        new OperatorFailedEvent(taskExecutionId, operatorName, inputs, message, todoPath));
    return OperatorResult.failure(message, inputs);
  }

  public String getName() {
    return spec().name();
  }

  /**
   * Subclasses implement this. Can throw exceptions which are caught above.
   */
  protected abstract OperatorResult doExecute(Map<String, Object> normalizedInputs);
}
