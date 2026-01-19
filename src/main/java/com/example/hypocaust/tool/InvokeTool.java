package com.example.hypocaust.tool;

import com.example.hypocaust.domain.OperatorLedger;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.RunContextHolder;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
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

  private final OperatorRegistry operatorRegistry;
  private final ModelCallLogger modelCallLogger;

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

        if (inputValue instanceof String) {
          inputValue = resolvePlaceholders(ledger.values(), (String) inputValue);
        }
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

  private String resolvePlaceholders(Map<String, Object> context, String template) {
    if (template == null || !template.contains("{{")) {
      return template; // Early exit if no placeholders
    }

    return PLACEHOLDER_PATTERN.matcher(template).replaceAll(matchResult -> {
      final var key = matchResult.group(1).trim();
      final var value = context.get(key);
      return value != null ? Matcher.quoteReplacement(value.toString()) : matchResult.group(0);
    });
  }
}
