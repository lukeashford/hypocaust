package com.example.the_machine.operator;

import com.example.the_machine.service.RunContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Heuristic-based remediator that applies common remediation patterns: - Timeout/backoff
 * adjustments - Clamp out-of-range values to min/max bounds - Swap model values within enum
 * constraints
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class HeuristicRemediator implements Remediator {

  private final ObjectMapper objectMapper;

  @Override
  public List<JsonNode> remediate(RunContext ctx, Map<String, Object> normalizedInputs,
      Exception exception, String remediationHints) {
    final var patches = new ArrayList<JsonNode>();
    final var exceptionMessage = exception.getMessage().toLowerCase();

    log.debug("HeuristicRemediator attempting remediation for exception: {}",
        exception.getMessage());

    // Timeout-related remediation
    if (exceptionMessage.contains("timeout") || exceptionMessage.contains("timed out")) {
      patches.addAll(adjustTimeouts(normalizedInputs));
    }

    // Rate limit remediation
    if (exceptionMessage.contains("rate limit") || exceptionMessage.contains("429")) {
      patches.addAll(addBackoff(normalizedInputs));
    }

    // Range/validation error remediation
    if (exceptionMessage.contains("out of range") || exceptionMessage.contains("invalid value")) {
      patches.addAll(clampValues(normalizedInputs));
    }

    // Model availability remediation
    if (exceptionMessage.contains("model") && exceptionMessage.contains("unavailable")) {
      patches.addAll(switchModel(normalizedInputs));
    }

    log.debug("HeuristicRemediator generated {} patches", patches.size());
    return patches;
  }

  private List<JsonNode> adjustTimeouts(Map<String, Object> inputs) {
    final var patches = new ArrayList<JsonNode>();

    for (final var entry : inputs.entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();

      if (key.toLowerCase().contains("timeout") && value instanceof Number) {
        final var currentTimeout = ((Number) value).intValue();
        final var newTimeout = Math.min(currentTimeout * 2, 300); // Cap at 5 minutes

        final var patch = createReplacePatch("/" + key, newTimeout);
        patches.add(patch);
        log.debug("Adjusting timeout {} from {} to {}", key, currentTimeout, newTimeout);
      }
    }

    return patches;
  }

  private List<JsonNode> addBackoff(Map<String, Object> inputs) {
    final var patches = new ArrayList<JsonNode>();

    // Add or increase retry delay
    if (!inputs.containsKey("retryDelayMs")) {
      final var patch = objectMapper.createObjectNode();
      patch.put("op", "add");
      patch.put("path", "/retryDelayMs");
      patch.set("value", objectMapper.valueToTree(1000));
      patches.add(patch);
      log.debug("Adding retryDelayMs: 1000");
    } else if (inputs.get("retryDelayMs") instanceof Number) {
      final var currentDelay = ((Number) inputs.get("retryDelayMs")).intValue();
      final var newDelay = Math.min(currentDelay * 2, 30000); // Cap at 30 seconds

      final var patch = createReplacePatch("/retryDelayMs", newDelay);
      patches.add(patch);
      log.debug("Increasing retryDelayMs from {} to {}", currentDelay, newDelay);
    }

    return patches;
  }

  private List<JsonNode> clampValues(Map<String, Object> inputs) {
    final var patches = new ArrayList<JsonNode>();

    for (final var entry : inputs.entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();

      if (value instanceof Number) {
        final var numValue = ((Number) value).doubleValue();
        Double clampedValue = null;

        // Common parameter ranges
        if (key.equals("temperature")) {
          clampedValue = Math.max(0.0, Math.min(2.0, numValue));
        } else if (key.equals("maxTokens")) {
          clampedValue = (double) Math.max(1, Math.min(4096, (int) numValue));
        } else if (key.contains("limit") || key.contains("max")) {
          clampedValue = Math.max(1.0, numValue);
        }

        if (clampedValue != null && !clampedValue.equals(numValue)) {
          final var patch = createReplacePatch("/" + key, clampedValue);
          patches.add(patch);
          log.debug("Clamping {} from {} to {}", key, numValue, clampedValue);
        }
      }
    }

    return patches;
  }

  private List<JsonNode> switchModel(Map<String, Object> inputs) {
    final var patches = new ArrayList<JsonNode>();

    if (inputs.containsKey("model") && inputs.get("model") instanceof String currentModel) {
      String fallbackModel = getFallbackModel(currentModel);

      if (fallbackModel != null && !fallbackModel.equals(currentModel)) {
        final var patch = createReplacePatch("/model", fallbackModel);
        patches.add(patch);
        log.debug("Switching model from {} to {}", currentModel, fallbackModel);
      }
    }

    return patches;
  }

  private String getFallbackModel(String currentModel) {
    // Simple fallback mapping - in real implementation this would be more sophisticated
    return switch (currentModel.toLowerCase()) {
      case "gpt-4" -> "gpt-3.5-turbo";
      case "gpt-4-turbo" -> "gpt-4";
      case "claude-3-opus" -> "claude-3-sonnet";
      case "claude-3-sonnet" -> "claude-3-haiku";
      default -> null;
    };
  }

  private ObjectNode createReplacePatch(String path, Object value) {
    final var patch = objectMapper.createObjectNode();
    patch.put("op", "replace");
    patch.put("path", path);
    patch.set("value", objectMapper.valueToTree(value));
    return patch;
  }

}