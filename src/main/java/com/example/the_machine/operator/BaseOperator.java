package com.example.the_machine.operator;

import com.example.the_machine.dto.RunDto;
import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.operator.result.OperatorResultCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Base implementation of Operator that provides common lifecycle management including validation,
 * defaults, budget checks, timing, retry logic, and schema-aware remediation. Concrete operators
 * should extend this class and implement doExecute().
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseOperator implements Operator {

  private final ObjectMapper objectMapper;
  private final List<Remediator> remediators;

  /**
   * Returns the version of this operator implementation. Should follow semantic versioning (e.g.,
   * "1.0.0", "2.1.3").
   *
   * @return the operator version
   */
  protected abstract String getVersion();

  @Override
  public final OperatorResult execute(RunDto ctx, Map<String, Object> rawInputs) {
    final var startTime = Instant.now();
    final var spec = spec();
    final var operatorName = this.getClass().getSimpleName();
    final var operatorVersion = getVersion();

    log.debug("Starting execution of {} with inputs: {}", operatorName, rawInputs.keySet());

    try {
      // Step 1: Validate raw inputs against ToolSpec
      final var validationResult = spec.validate(rawInputs);
      if (!validationResult.isOk()) {
        log.warn("Validation failed for {}: {}", operatorName, validationResult.getMessage());
        return OperatorResult.validationFailure(operatorName, operatorVersion,
            validationResult.getMessage());
      }

      // Step 2: Apply defaults
      final var normalizedInputs = spec.applyDefaults(rawInputs);
      log.debug("Applied defaults, normalized inputs: {}", normalizedInputs.keySet());

      // Step 3: Check budgets
      // TODO

      // Step 4: Execute with retry logic
      return executeWithRetries(ctx, normalizedInputs, operatorName, operatorVersion, startTime);

    } catch (Exception e) {
      final var latencyMs = calculateLatency(startTime);
      log.error("Unexpected error in {} after {}ms", operatorName, latencyMs, e);

      // Redact secrets from error message
      final var redactedMessage = redactSecrets(e.getMessage(), rawInputs);

      return OperatorResult.failure(operatorName, operatorVersion,
              OperatorResultCode.UNEXPECTED_ERROR,
              redactedMessage, new HashMap<>(rawInputs))
          .withMetrics(Map.of("latencyMs", latencyMs));
    }
  }

  /**
   * Execute with retry and remediation logic using 0-based attempt indexing.
   */
  private OperatorResult executeWithRetries(RunDto ctx, Map<String, Object> normalizedInputs,
      String operatorName, String operatorVersion, Instant startTime) {
    final var allPatches = new ArrayList<JsonNode>();
    final var currentInputs = new HashMap<>(normalizedInputs);
    final var maxTries = 3; // TODO configurable

    Exception lastException = null;
    String lastErrorSignature = null;
    int attemptsForCurrentError = 0;

    for (int attempt = 0; attempt < maxTries; attempt++) {
      try {
        log.debug("Attempt {}/{} for {}", attempt, maxTries - 1, operatorName);

        final var result = doExecute(ctx, currentInputs);

        // Success path
        final var latencyMs = calculateLatency(startTime);
        log.debug("Successfully executed {} in {}ms after {} attempts",
            operatorName, latencyMs, attempt + 1);

        return OperatorResult.success(operatorName, operatorVersion, normalizedInputs,
                result.getOutputs())
            .withMetrics(Map.of("latencyMs", latencyMs))
            .withAttempts(attempt + 1)
            .withRemediationPatches(allPatches);

      } catch (Exception e) {
        lastException = e;
        final var currentErrorSignature = createErrorSignature(e);
        log.warn("Attempt {}/{} failed for {}: {}", attempt, maxTries - 1, operatorName,
            e.getMessage());

        if (attempt < maxTries - 1) {
          // Check if error signature changed - if so, restart with first remediator
          if (!currentErrorSignature.equals(lastErrorSignature)) {
            log.debug("Error signature changed from '{}' to '{}', restarting with first remediator",
                lastErrorSignature, currentErrorSignature);
            attemptsForCurrentError = 0;
            lastErrorSignature = currentErrorSignature;
          }

          // Check if we've tried all remediators for this error
          if (attemptsForCurrentError >= remediators.size()) {
            log.info("Tried all {} remediators for error '{}', stopping early at attempt {}",
                remediators.size(), currentErrorSignature, attempt);
            break;
          }

          // Try remediation with current remediator
          final var remediationPatches = getPatchFromRemediator(ctx, currentInputs, e,
              attemptsForCurrentError);
          if (remediationPatches.isEmpty()) {
            log.debug("No remediation available from remediator {}, will retry with same inputs",
                attemptsForCurrentError);
          } else {
            applyPatches(currentInputs, remediationPatches); // Now modifies in-place
            allPatches.addAll(remediationPatches);
            log.debug("Applied {} remediation patches from remediator {} for {}",
                remediationPatches.size(), attemptsForCurrentError, operatorName);
          }

          attemptsForCurrentError++;
        }
      }
    }

    // All retries exhausted or early termination - create failure result
    final var latencyMs = calculateLatency(startTime);
    final var redactedMessage = redactSecrets(lastException.getMessage(), normalizedInputs);

    return OperatorResult.failure(operatorName, operatorVersion,
            OperatorResultCode.EXECUTION_FAILED, redactedMessage, normalizedInputs)
        .withMetrics(Map.of("latencyMs", latencyMs))
        .withAttempts(maxTries)
        .withRemediationPatches(allPatches);
  }

  /**
   * Gets patch from a specific remediator using direct indexing.
   */
  private List<JsonNode> getPatchFromRemediator(RunDto ctx,
      Map<String, Object> normalizedInputs,
      Exception exception, int remediatorIndex) {

    if (remediators.isEmpty() || remediatorIndex >= remediators.size()) {
      return List.of();
    }

    final var remediator = remediators.get(remediatorIndex);
    final var hints = remediationHints();

    try {
      final var patches = remediator.remediate(ctx, normalizedInputs, exception, hints);
      log.debug("Remediator {} ({}) generated {} patches",
          remediatorIndex, remediator.getName(), patches.size());
      return patches;
    } catch (Exception remediationError) {
      log.warn("Remediator {} ({}) failed: {}",
          remediatorIndex, remediator.getName(), remediationError.getMessage());
      return List.of();
    }
  }

  /**
   * Creates a signature for an error to detect if we're getting the same error repeatedly.
   */
  private String createErrorSignature(Exception e) {
    // Use exception class name and first 100 chars of message for signature
    final var message = e.getMessage();
    final var truncatedMessage = message != null && message.length() > 100 ?
        message.substring(0, 100) : (message != null ? message : "");
    return e.getClass().getSimpleName() + ":" + truncatedMessage;
  }

  /**
   * Applies simple JSON patches to input map in-place. Supports "replace", "add", and "remove"
   * operations.
   */
  private void applyPatches(Map<String, Object> inputs, List<JsonNode> patches) {
    try {
      // Apply each patch directly to the input map
      for (final var patchNode : patches) {
        if (!patchNode.has("op") || !patchNode.has("path")) {
          log.warn("Invalid patch format, skipping: {}", patchNode);
          continue;
        }

        final var operation = patchNode.get("op").asText();
        final var path = patchNode.get("path").asText();

        // Simple path handling - only support root level fields for now
        if (!path.startsWith("/") || path.indexOf("/", 1) != -1) {
          log.warn("Only root-level paths supported, skipping: {}", path);
          continue;
        }

        final var fieldName = path.substring(1); // Remove leading "/"

        switch (operation) {
          case "replace":
          case "add":
            if (patchNode.has("value")) {
              final var value = objectMapper.convertValue(patchNode.get("value"), Object.class);
              inputs.put(fieldName, value);
              log.debug("Applied {} patch: {} = {}", operation, fieldName, value);
            }
            break;

          case "remove":
            inputs.remove(fieldName);
            log.debug("Applied remove patch: {}", fieldName);
            break;

          default:
            log.warn("Unsupported patch operation: {}", operation);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to apply remediation patches: {}", e.getMessage());
      // Input map remains unchanged if patching fails
    }
  }

  /**
   * Redacts secrets from error messages based on parameter specifications.
   */
  private String redactSecrets(String message, Map<String, Object> inputs) {
    if (message == null) {
      return null;
    }

    return spec().getInputs().stream()
        .filter(param -> param.isSecret() && inputs.containsKey(param.getName()))
        .map(param -> inputs.get(param.getName()))
        .filter(value -> value != null && !value.toString().isEmpty())
        .map(Object::toString)
        .reduce(message, (msg, secretValue) -> msg.replace(secretValue, "[REDACTED]"));
  }

  /**
   * Calculates latency from start time to now.
   */
  private long calculateLatency(Instant startTime) {
    return Instant.now().toEpochMilli() - startTime.toEpochMilli();
  }

  /**
   * Concrete operators implement their specific logic here. This method will be called within the
   * retry loop managed by execute().
   *
   * @param ctx the run context
   * @param inputs the validated and normalized inputs (may include remediation patches)
   * @return the operator result with outputs
   * @throws Exception if the operation fails
   */
  protected abstract OperatorResult doExecute(RunDto ctx, Map<String, Object> inputs)
      throws Exception;

  /**
   * Override this method to provide remediation hints to remediators. Hints can guide the
   * remediation strategy.
   *
   * @return remediation hints as a string, or null if no specific hints
   */
  protected String remediationHints() {
    return null;
  }
}