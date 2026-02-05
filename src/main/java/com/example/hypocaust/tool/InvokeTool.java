package com.example.hypocaust.tool;

import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.OperatorLedger;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoStatus;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.TaskExecutionContextHolder;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    return invoke(ledger, null);
  }

  public OperatorResult invoke(OperatorLedger ledger, UUID parentTodoId) {
    final var indent = TaskExecutionContextHolder.getIndent();
    log.info("{}Starting operator chain with {} children under parent {}",
        indent, ledger.children().size(), parentTodoId);

    // ID propagation logic:
    // - Multiple children: create todos for each, pass their IDs down
    // - Single child: propagate the same parent ID (no subtask publishing)
    boolean singleChild = ledger.children().size() == 1;

    // Create todos and track their IDs
    var childTodoIds = new ArrayList<UUID>();
    if (!singleChild) {
      // Publish all subtasks at once at the beginning
      var subtasks = new ArrayList<Todo>();
      for (int i = 0; i < ledger.children().size(); i++) {
        var child = ledger.children().get(i);
        var todoId = UUID.randomUUID();
        childTodoIds.add(todoId);
        var description = child.todo() != null ? child.todo() : child.operatorName();
        subtasks.add(new Todo(description, TodoStatus.PENDING));
      }
      TaskExecutionContextHolder.getContext().getTodos().addSubtasks(parentTodoId, subtasks);
    } else {
      // Single child keeps the same parent ID
      childTodoIds.add(parentTodoId);
    }

    for (int i = 0; i < ledger.children().size(); i++) {
      final var child = ledger.children().get(i);
      final var operatorName = child.operatorName();
      final var opOpt = operatorRegistry.get(operatorName);

      // Get the todo ID for this child
      final var childTodoId = childTodoIds.get(i);

      if (opOpt.isEmpty()) {
        final var error = "No operator found with name: " + operatorName;
        log.error("{} [FAILED] {}", indent, error);
        updateStatus(childTodoId, singleChild, TodoStatus.FAILED);
        return OperatorResult.failure(error, Map.of("operatorName", operatorName));
      }

      final var op = opOpt.get();

      log.info("{}[START] {} (inputs: {})",
          indent,
          operatorName,
          child.inputsToKeys().keySet());

      updateStatus(childTodoId, singleChild, TodoStatus.IN_PROGRESS);

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
      // Pass the todoId to the operator
      final var result = op.execute(inputs, childTodoId);
      TaskExecutionContextHolder.decrementDepth();

      if (!result.ok()) {
        log.error("{}[FAILED] {}: {}", indent, child.operatorName(), result.message());
        updateStatus(childTodoId, singleChild, TodoStatus.FAILED);
        return result;
      }

      log.info("{}[DONE] {}", indent, child.operatorName());

      updateStatus(childTodoId, singleChild, TodoStatus.COMPLETED);

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
   * Resolve an @artifact: reference to the full artifact structure. Returns PendingArtifact if
   * found in pending changes, otherwise ArtifactDto from current state.
   *
   * @param artifactRef the artifact reference string (e.g., "@artifact:hero_image")
   * @return the artifact structure (PendingArtifact or ArtifactDto), or the name string if not
   * found
   */
  private Object resolveArtifact(String artifactRef) {
    var artifactName = artifactRef.substring(ARTIFACT_PREFIX.length()).trim();
    log.debug("Resolving artifact reference: '{}'", artifactName);

    return TaskExecutionContextHolder.getContext().getArtifacts().get(artifactName);
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
      // If the value is an Artifact, return its description
      if (value instanceof Artifact dto) {
        return Matcher.quoteReplacement(dto.description());
      }
      return Matcher.quoteReplacement(value.toString());
    });
  }

  /**
   * Helper method to update task status only for multiple-child ledgers. Eliminates repeated
   * conditional checks throughout the invoke loop.
   */
  private void updateStatus(UUID todoId, boolean singleChild, TodoStatus status) {
    if (!singleChild && todoId != null) {
      TaskExecutionContextHolder.getContext().getTodos().updateStatus(todoId, status);
    }
  }
}
