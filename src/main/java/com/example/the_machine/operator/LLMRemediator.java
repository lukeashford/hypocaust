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
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * LLM-based remediator that uses language model capabilities to propose sophisticated JSON Patch
 * remediation strategies constrained to adjustable fields and schema bounds. This is an optional
 * implementation that could integrate with actual LLM services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMRemediator implements Remediator {

  private final ObjectMapper objectMapper;

  @Override
  public List<JsonNode> remediate(RunContext ctx, Map<String, Object> normalizedInputs,
      Exception exception, String remediationHints) {
    val patches = new ArrayList<JsonNode>();

    log.debug("LLMRemediator attempting remediation for exception: {}", exception.getMessage());

    // In a real implementation, this would call an LLM service with a prompt like:
    // "Given the error: '{}' and inputs: '{}' with hints: '{}',
    //  propose JSON patches constrained to adjustable fields."

    // For now, we'll implement some rule-based logic that simulates LLM reasoning
    patches.addAll(analyzeAndRemediate(normalizedInputs, exception, remediationHints));

    log.debug("LLMRemediator generated {} patches", patches.size());
    return patches;
  }

  private List<JsonNode> analyzeAndRemediate(Map<String, Object> inputs, Exception exception,
      String hints) {
    val patches = new ArrayList<JsonNode>();
    val errorMessage = exception.getMessage().toLowerCase();

    // Simulate LLM reasoning for complex parameter adjustments
    if (errorMessage.contains("context length") || errorMessage.contains("too long")) {
      patches.addAll(reduceContextLength(inputs));
    }

    if (errorMessage.contains("invalid format") || errorMessage.contains("schema")) {
      patches.addAll(fixFormatIssues(inputs, hints));
    }

    if (errorMessage.contains("permission") || errorMessage.contains("unauthorized")) {
      patches.addAll(adjustPermissionParams(inputs));
    }

    if (errorMessage.contains("resource") && errorMessage.contains("limit")) {
      patches.addAll(optimizeResourceUsage(inputs));
    }

    return patches;
  }

  private List<JsonNode> reduceContextLength(Map<String, Object> inputs) {
    val patches = new ArrayList<JsonNode>();

    // Reduce max tokens if present
    if (inputs.containsKey("maxTokens") && inputs.get("maxTokens") instanceof Number) {
      val currentMax = ((Number) inputs.get("maxTokens")).intValue();
      val reducedMax = Math.max(100, currentMax / 2);

      val patch = createReplacePatch("/maxTokens", reducedMax);
      patches.add(patch);
      log.debug("LLM analysis: Reducing maxTokens from {} to {}", currentMax, reducedMax);
    }

    // Adjust context window parameters
    if (inputs.containsKey("contextSize") && inputs.get("contextSize") instanceof Number) {
      val currentSize = ((Number) inputs.get("contextSize")).intValue();
      val reducedSize = Math.max(1024, currentSize * 3 / 4);

      val patch = createReplacePatch("/contextSize", reducedSize);
      patches.add(patch);
      log.debug("LLM analysis: Reducing contextSize from {} to {}", currentSize, reducedSize);
    }

    return patches;
  }

  private List<JsonNode> fixFormatIssues(Map<String, Object> inputs, String hints) {
    val patches = new ArrayList<JsonNode>();

    // Use hints to guide format corrections
    if (hints != null && hints.contains("json")) {
      if (inputs.containsKey("responseFormat")) {
        val patch = createReplacePatch("/responseFormat", "json_object");
        patches.add(patch);
        log.debug("LLM analysis: Setting responseFormat to json_object based on hints");
      }
    }

    // Fix common formatting parameters
    if (inputs.containsKey("format") && !(inputs.get("format") instanceof String)) {
      val patch = createReplacePatch("/format", "text");
      patches.add(patch);
      log.debug("LLM analysis: Correcting format parameter type");
    }

    return patches;
  }

  private List<JsonNode> adjustPermissionParams(Map<String, Object> inputs) {
    val patches = new ArrayList<JsonNode>();

    // Remove or adjust parameters that might cause permission issues
    if (inputs.containsKey("systemMessage")) {
      val patch = createRemovePatch("/systemMessage");
      patches.add(patch);
      log.debug("LLM analysis: Removing systemMessage due to permission issues");
    }

    // Switch to more permissive model if available
    if (inputs.containsKey("model") && inputs.get("model") instanceof String currentModel) {
      if (currentModel.contains("restricted") || currentModel.contains("private")) {
        val patch = createReplacePatch("/model", "gpt-3.5-turbo");
        patches.add(patch);
        log.debug("LLM analysis: Switching to more accessible model");
      }
    }

    return patches;
  }

  private List<JsonNode> optimizeResourceUsage(Map<String, Object> inputs) {
    val patches = new ArrayList<JsonNode>();

    // Reduce resource-intensive parameters
    if (inputs.containsKey("temperature") && inputs.get("temperature") instanceof Number) {
      val currentTemp = ((Number) inputs.get("temperature")).doubleValue();
      if (currentTemp > 1.0) {
        val patch = createReplacePatch("/temperature", 0.7);
        patches.add(patch);
        log.debug("LLM analysis: Reducing temperature to optimize resources");
      }
    }

    if (inputs.containsKey("numCompletions") && inputs.get("numCompletions") instanceof Number) {
      val currentNum = ((Number) inputs.get("numCompletions")).intValue();
      if (currentNum > 1) {
        val patch = createReplacePatch("/numCompletions", 1);
        patches.add(patch);
        log.debug("LLM analysis: Reducing numCompletions to save resources");
      }
    }

    return patches;
  }

  private ObjectNode createReplacePatch(String path, Object value) {
    val patch = objectMapper.createObjectNode();
    patch.put("op", "replace");
    patch.put("path", path);
    patch.set("value", objectMapper.valueToTree(value));
    return patch;
  }

  private ObjectNode createRemovePatch(String path) {
    val patch = objectMapper.createObjectNode();
    patch.put("op", "remove");
    patch.put("path", path);
    return patch;
  }
}