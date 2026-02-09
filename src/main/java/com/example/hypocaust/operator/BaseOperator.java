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
   * Template method that wraps execution with error handling.
   *
   * @param rawInputs The raw inputs for the operator
   * @param todoId The ID of the todo in the task tree (null for root level)
   */
  public final OperatorResult execute(Map<String, Object> rawInputs, UUID todoId) {
    final var taskExecutionId = TaskExecutionContextHolder.getTaskExecutionId();
    final var operatorName = getName();

    TaskExecutionContextHolder.pushTodoId(todoId);
    TaskExecutionContextHolder.markCurrentTodoRunning();
    try {
      // First normalize (apply defaults), then validate
      final var normalizedInputs = spec().normalize(rawInputs);

      eventService.publish(
          new OperatorStartedEvent(taskExecutionId, operatorName, normalizedInputs));

      final var validationResult = spec().validate(normalizedInputs);
      if (!validationResult.ok()) {
        final var failure = OperatorResult.failure(validationResult.message(), normalizedInputs);
        eventService.publish(
            new OperatorFailedEvent(taskExecutionId, operatorName, normalizedInputs,
                failure.message()));
        TaskExecutionContextHolder.markCurrentTodoFailed();
        return failure;
      }

      final var result = doExecute(normalizedInputs);

      if (result.ok()) {
        eventService.publish(
            new OperatorFinishedEvent(taskExecutionId, operatorName, normalizedInputs,
                result.outputs()));
        TaskExecutionContextHolder.markCurrentTodoCompleted();
      } else {
        eventService.publish(
            new OperatorFailedEvent(taskExecutionId, operatorName, normalizedInputs,
                result.message()));
        TaskExecutionContextHolder.markCurrentTodoFailed();
      }

      return result;
    } catch (ArtifactNotFoundException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoId,
          "Artifact not found: " + e.getMessage());
    } catch (ArtifactExistsException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoId,
          "Artifact already exists: " + e.getArtifactName());
    } catch (ArtifactTypeMismatchException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoId,
          "Type mismatch: " + e.getMessage());
    } catch (ExternalServiceException e) {
      return handleException(taskExecutionId, operatorName, rawInputs, todoId,
          "External service error: " + e.getMessage());
    } catch (Exception e) {
      log.error("Unexpected error in operator {}: {}", operatorName, e.getMessage(), e);
      return handleException(taskExecutionId, operatorName, rawInputs, todoId,
          "Unexpected error: " + e.getMessage());
    } finally {
      TaskExecutionContextHolder.popTodoId();
    }
  }

  private OperatorResult handleException(UUID taskExecutionId, String operatorName,
      Map<String, Object> inputs, UUID todoId, String message) {
    eventService.publish(
        new OperatorFailedEvent(taskExecutionId, operatorName, inputs, message));
    TaskExecutionContextHolder.markCurrentTodoFailed();
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
