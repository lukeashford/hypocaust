package com.example.hypocaust.tool;

import com.example.hypocaust.domain.ArtifactNode;
import com.example.hypocaust.domain.OperatorLedger;
import com.example.hypocaust.exception.AnchorNotFoundException;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.ExecutionContextHolder;
import com.example.hypocaust.operator.RunContextHolder;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.ArtifactGraphService;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvokeTool {

  private static final String ANCHOR_PREFIX = "@anchor:";

  private final OperatorRegistry operatorRegistry;
  private final ModelCallLogger modelCallLogger;
  private final ArtifactGraphService artifactGraphService;

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

  @Tool(name = "invoke", description = "Invoke a chain of operators, as specified in the ledger")
  public OperatorResult invoke(OperatorLedger ledger) {
    final var indent = RunContextHolder.getIndent();
    log.info("{}┌─ Starting operator chain with {} children", indent, ledger.children().size());

    for (final var child : ledger.children()) {
      final var operatorName = child.operatorName();
      final var opOpt = operatorRegistry.get(operatorName);

      if (opOpt.isEmpty()) {
        final var error = "No operator found with name: " + operatorName;
        log.error("{}{} [FAILED] {}", indent, indent, error);
        return OperatorResult.failure(error, Map.of("operatorName", operatorName));
      }

      final var op = opOpt.get();

      log.info("{}├─ [START] {} (inputs: {})",
          indent,
          operatorName,
          child.inputsToKeys().keySet());

      final var inputs = new HashMap<String, Object>();
      for (final var inputName : op.spec().getInputKeys()) {
        final var inputKey = child.inputsToKeys().get(inputName);
        var inputValue = ledger.values().get(inputKey);

        // Resolve @anchor: references and placeholders
        inputValue = resolveValue(ledger.values(), inputValue);
        inputs.put(inputName, inputValue);
      }

      // Increment depth before executing child operator
      RunContextHolder.incrementDepth();
      final var result = op.execute(inputs);
      RunContextHolder.decrementDepth();

      if (!result.ok()) {
        log.error("{}├─ [FAILED] {}: {}", indent, child.operatorName(), result.message());
        return result;
      }

      log.info("{}├─ [DONE] {}", indent, child.operatorName());

      for (final var outputName : op.spec().getOutputKeys()) {
        final var outputKey = child.outputsToKeys().get(outputName);
        if (outputKey == null) {
          continue;
        }
        if (ledger.values().containsKey(outputKey)) {
          return OperatorResult.failure(
              "Output key already exists: " + outputKey,
              inputs
          );
        }

        ledger.values().put(outputKey, result.outputs().get(outputName));
      }
    }

    // Log the final ledger state
    log.info("{}└─ Operator chain completed", indent);
    modelCallLogger.logLedger(ledger);

    final var finalValue = ledger.values().get(ledger.finalOutputKey());
    if (finalValue == null) {
      return OperatorResult.failure(
          "Final output key not found in ledger values: " + ledger.finalOutputKey(),
          ledger.values()
      );
    }

    return OperatorResult.success(
        "Successfully invoked operator chain",
        Map.of("task", ledger.values().getOrDefault("task", "unknown")),
        Map.of("result", finalValue)
    );
  }

  /**
   * Resolve a value, handling both @anchor: references and {{placeholder}} patterns.
   */
  private Object resolveValue(Map<String, Object> context, Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof String strValue) {
      // Check for @anchor: prefix
      if (strValue.startsWith(ANCHOR_PREFIX)) {
        return resolveAnchor(strValue);
      }

      // Resolve placeholders
      return resolvePlaceholders(context, strValue);
    }

    return value;
  }

  /**
   * Resolve an @anchor: reference to an ArtifactNode.
   * The anchor query is the part after "@anchor:".
   */
  private ArtifactNode resolveAnchor(String anchorRef) {
    var anchorQuery = anchorRef.substring(ANCHOR_PREFIX.length()).trim();
    log.debug("Resolving anchor reference: '{}'", anchorQuery);

    // Check if ExecutionContext is available
    if (!ExecutionContextHolder.hasContext()) {
      // Fall back to using ArtifactGraphService directly with RunContextHolder
      var projectId = RunContextHolder.getProjectId();
      try {
        var node = artifactGraphService.resolveAnchor(projectId, anchorQuery);
        log.info("Resolved anchor '{}' to artifact: {} (v{})",
            anchorQuery, node.id(), node.version());
        return node;
      } catch (AnchorNotFoundException e) {
        log.warn("Failed to resolve anchor '{}': {}", anchorQuery, e.getMessage());
        throw e;
      }
    }

    // Use ExecutionContext for resolution
    var context = ExecutionContextHolder.getContext();
    var results = context.findByDescription(anchorQuery);

    if (!results.isEmpty()) {
      var node = results.get(0);
      log.info("Resolved anchor '{}' to artifact: {} (v{})",
          anchorQuery, node.id(), node.version());
      return node;
    }

    // Fall back to ArtifactGraphService for semantic search
    try {
      var node = artifactGraphService.resolveAnchor(context.projectId(), anchorQuery);
      log.info("Resolved anchor '{}' to artifact: {} (v{})",
          anchorQuery, node.id(), node.version());
      return node;
    } catch (AnchorNotFoundException e) {
      log.warn("Failed to resolve anchor '{}': {}", anchorQuery, e.getMessage());
      throw e;
    }
  }

  private String resolvePlaceholders(Map<String, Object> context, String template) {
    if (template == null || !template.contains("{{")) {
      return template; // Early exit if no placeholders
    }

    return PLACEHOLDER_PATTERN.matcher(template).replaceAll(matchResult -> {
      final var key = matchResult.group(1).trim();
      final var value = context.get(key);
      if (value == null) {
        return matchResult.group(0);
      }
      // If the value is an ArtifactNode, convert to a useful string representation
      if (value instanceof ArtifactNode node) {
        return Matcher.quoteReplacement(node.anchor().description());
      }
      return Matcher.quoteReplacement(value.toString());
    });
  }
}
