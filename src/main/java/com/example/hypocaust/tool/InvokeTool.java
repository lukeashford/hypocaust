package com.example.hypocaust.tool;

import com.example.hypocaust.domain.OperatorLedger;
import com.example.hypocaust.domain.TaskItem;
import com.example.hypocaust.domain.TaskStatus;
import com.example.hypocaust.dto.ArtifactDto;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tool for invoking operator chains defined via OperatorLedger.
 *
 * <h2>OperatorLedger Structure</h2>
 * <pre>
 * record OperatorLedger(
 *     Map&lt;String, Object&gt; values,       // Initial inputs + templates with {{key}} references
 *     List&lt;ChildConfig&gt; children,       // Ordered operator invocations
 *     String finalOutputKey             // Key holding the final result
 * ) {
 *   record ChildConfig(
 *       String operatorName,            // Operator to invoke
 *       String todo,                    // Human-readable task description (REQUIRED)
 *       Map&lt;String, String&gt; inputsToKeys,
 *       Map&lt;String, String&gt; outputsToKeys
 *   ) {}
 * }
 * </pre>
 *
 * <h2>Value Resolution</h2>
 * <ul>
 *   <li>{@code {{keyName}}} - References a value from the ledger's values map</li>
 *   <li>{@code @artifact:name} - References an artifact by its semantic name</li>
 * </ul>
 *
 * <h2>Task Progress Integration</h2>
 * <p>The {@code todo} field in ChildConfig is used to populate the task progress tree.
 * Each child's todo becomes the human-readable description shown to users during execution.
 * For example: "Generate hero portrait image", "Compile final video".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvokeTool {

  private static final String ARTIFACT_PREFIX = "@artifact:";
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

  private final OperatorRegistry operatorRegistry;
  private final ModelCallLogger modelCallLogger;

  @Tool(name = "invoke", description = "Invoke a chain of operators, as specified in the ledger")
  public OperatorResult invoke(OperatorLedger ledger) {
    return invoke(ledger, "0");
  }

  public OperatorResult invoke(OperatorLedger ledger, String todoPath) {
    final var indent = TaskExecutionContextHolder.getIndent();
    log.info("{}Starting operator chain with {} children at path {}",
        indent, ledger.children().size(), todoPath);

    // Path propagation logic:
    // - Multiple children: extend the path (e.g., "0.1" -> "0.1.0", "0.1.1", ...)
    // - Single child: propagate the same path (no subtask publishing)
    boolean singleChild = ledger.children().size() == 1;

    if (!singleChild) {
      // Publish all subtasks at once at the beginning
      var subtasks = new ArrayList<TaskItem>();
      for (int i = 0; i < ledger.children().size(); i++) {
        var child = ledger.children().get(i);
        var childPath = todoPath + "." + i;
        var description = child.todo() != null ? child.todo() : child.operatorName();
        subtasks.add(new TaskItem(childPath, description, TaskStatus.PENDING));
      }
      TaskExecutionContextHolder.getContext().publishSubtasks(todoPath, subtasks);
    }

    for (int i = 0; i < ledger.children().size(); i++) {
      final var child = ledger.children().get(i);
      final var operatorName = child.operatorName();
      final var opOpt = operatorRegistry.get(operatorName);

      // Single child keeps the same path; multiple children extend it
      final var childPath = singleChild ? todoPath : todoPath + "." + i;

      if (opOpt.isEmpty()) {
        final var error = "No operator found with name: " + operatorName;
        log.error("{} [FAILED] {}", indent, error);
        updateStatus(childPath, singleChild, TaskStatus.FAILED);
        return OperatorResult.failure(error, Map.of("operatorName", operatorName));
      }

      final var op = opOpt.get();

      log.info("{}[START] {} (inputs: {})",
          indent,
          operatorName,
          child.inputsToKeys().keySet());

      updateStatus(childPath, singleChild, TaskStatus.IN_PROGRESS);

      final var inputs = new HashMap<String, Object>();
      for (final var inputName : op.spec().getInputKeys()) {
        final var inputKey = child.inputsToKeys().get(inputName);
        var inputValue = ledger.values().get(inputKey);

        // Resolve @artifact: references and placeholders
        inputValue = resolveValue(ledger.values(), inputValue);
        inputs.put(inputName, inputValue);
      }

      // Increment depth before executing child operator
      TaskExecutionContextHolder.incrementDepth();
      // Pass the todoPath to the operator
      final var result = op.execute(inputs, childPath);
      TaskExecutionContextHolder.decrementDepth();

      if (!result.ok()) {
        log.error("{}[FAILED] {}: {}", indent, child.operatorName(), result.message());
        updateStatus(childPath, singleChild, TaskStatus.FAILED);
        return result;
      }

      log.info("{}[DONE] {}", indent, child.operatorName());

      updateStatus(childPath, singleChild, TaskStatus.COMPLETED);

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
    log.info("{}Operator chain completed", indent);
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
   * Resolve a value, handling both @artifact: references and {{placeholder}} patterns.
   */
  private Object resolveValue(Map<String, Object> context, Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof String strValue) {
      // Check for @artifact: prefix (name-based lookup)
      if (strValue.startsWith(ARTIFACT_PREFIX)) {
        return resolveArtifact(strValue);
      }

      // Resolve placeholders
      return resolvePlaceholders(context, strValue);
    }

    return value;
  }

  /**
   * Resolve an @artifact: reference to the full artifact structure.
   * Returns PendingArtifact if found in pending changes, otherwise ArtifactDto from current state.
   *
   * @param artifactRef the artifact reference string (e.g., "@artifact:hero_image")
   * @return the artifact structure (PendingArtifact or ArtifactDto), or the name string if not found
   */
  private Object resolveArtifact(String artifactRef) {
    var artifactName = artifactRef.substring(ARTIFACT_PREFIX.length()).trim();
    log.debug("Resolving artifact reference: '{}'", artifactName);

    var ctx = TaskExecutionContextHolder.getContext();

    // First check pending changes
    var pending = ctx.getPending().getPendingArtifact(artifactName);
    if (pending.isPresent()) {
      log.debug("Resolved '{}' to pending artifact", artifactName);
      return pending.get();
    }

    // Then check current artifacts
    var currentArtifacts = ctx.getCurrentArtifacts();
    for (var artifact : currentArtifacts) {
      if (artifactName.equals(artifact.name())) {
        log.debug("Resolved '{}' to current artifact", artifactName);
        return artifact;
      }
    }

    // Not found - return the name for downstream handling
    log.warn("Artifact reference '{}' not found, returning name only", artifactName);
    return artifactName;
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
      // If the value is an ArtifactDto, return its description
      if (value instanceof ArtifactDto dto) {
        return Matcher.quoteReplacement(dto.description());
      }
      return Matcher.quoteReplacement(value.toString());
    });
  }

  /**
   * Helper method to update task status only for multiple-child ledgers.
   * Eliminates repeated conditional checks throughout the invoke loop.
   */
  private void updateStatus(String path, boolean singleChild, TaskStatus status) {
    if (!singleChild) {
      TaskExecutionContextHolder.getContext().updateTaskStatus(path, status);
    }
  }
}
