package com.example.the_machine.operator

import com.example.the_machine.service.RunContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * LLM-based remediator that uses language model capabilities to propose sophisticated JSON Patch
 * remediation strategies constrained to adjustable fields and schema bounds. This is an optional
 * implementation that could integrate with actual LLM services.
 */
@Component
class LLMRemediator : Remediator {

  companion object {

    private val log = KotlinLogging.logger {}
  }

  override fun remediate(
    ctx: RunContext,
    normalizedInputs: Map<String, Any>,
    exception: Exception,
    remediationHints: String?
  ): List<JsonElement> {
    val patches = mutableListOf<JsonElement>()

    log.debug("LLMRemediator attempting remediation for exception: {}", exception.message)

    // In a real implementation, this would call an LLM service with a prompt like:
    // "Given the error: '{}' and inputs: '{}' with hints: '{}',
    //  propose JSON patches constrained to adjustable fields."

    // For now, we'll implement some rule-based logic that simulates LLM reasoning
    patches.addAll(analyzeAndRemediate(normalizedInputs, exception, remediationHints))

    log.debug("LLMRemediator generated {} patches", patches.size)
    return patches
  }

  private fun analyzeAndRemediate(
    inputs: Map<String, Any>,
    exception: Exception,
    hints: String?
  ): List<JsonElement> {
    val patches = mutableListOf<JsonElement>()
    val errorMessage = exception.message?.lowercase() ?: ""

    // Simulate LLM reasoning for complex parameter adjustments
    when {
      errorMessage.contains("context length") || errorMessage.contains("too long") -> {
        patches.addAll(reduceContextLength(inputs))
      }

      errorMessage.contains("invalid format") || errorMessage.contains("schema") -> {
        patches.addAll(fixFormatIssues(inputs, hints))
      }

      errorMessage.contains("permission") || errorMessage.contains("unauthorized") -> {
        patches.addAll(adjustPermissionParams(inputs))
      }

      errorMessage.contains("resource") && errorMessage.contains("limit") -> {
        patches.addAll(optimizeResourceUsage(inputs))
      }
    }

    return patches
  }

  private fun reduceContextLength(inputs: Map<String, Any>): List<JsonElement> {
    val patches = mutableListOf<JsonElement>()

    // Reduce max tokens if present
    inputs["maxTokens"]?.let { value ->
      if (value is Number) {
        val currentMax = value.toInt()
        val reducedMax = maxOf(100, currentMax / 2)

        val patch = createReplacePatch("/maxTokens", reducedMax)
        patches.add(patch)
        log.debug("LLM analysis: Reducing maxTokens from {} to {}", currentMax, reducedMax)
      }
    }

    // Adjust context window parameters
    inputs["contextSize"]?.let { value ->
      if (value is Number) {
        val currentSize = value.toInt()
        val reducedSize = maxOf(1024, currentSize * 3 / 4)

        val patch = createReplacePatch("/contextSize", reducedSize)
        patches.add(patch)
        log.debug("LLM analysis: Reducing contextSize from {} to {}", currentSize, reducedSize)
      }
    }

    return patches
  }

  private fun fixFormatIssues(inputs: Map<String, Any>, hints: String?): List<JsonElement> {
    val patches = mutableListOf<JsonElement>()

    // Use hints to guide format corrections
    if (hints?.contains("json") == true && inputs.containsKey("responseFormat")) {
      val patch = createReplacePatch("/responseFormat", "json_object")
      patches.add(patch)
      log.debug("LLM analysis: Setting responseFormat to json_object based on hints")
    }

    // Fix common formatting parameters
    inputs["format"]?.let { value ->
      if (value !is String) {
        val patch = createReplacePatch("/format", "text")
        patches.add(patch)
        log.debug("LLM analysis: Correcting format parameter type")
      }
    }

    return patches
  }

  private fun adjustPermissionParams(inputs: Map<String, Any>): List<JsonElement> {
    val patches = mutableListOf<JsonElement>()

    // Remove or adjust parameters that might cause permission issues
    if (inputs.containsKey("systemMessage")) {
      val patch = createRemovePatch("/systemMessage")
      patches.add(patch)
      log.debug("LLM analysis: Removing systemMessage due to permission issues")
    }

    // Switch to more permissive model if available
    val currentModel = inputs["model"] as? String
    if (currentModel != null && (currentModel.contains("restricted") || currentModel.contains("private"))) {
      val patch = createReplacePatch("/model", "gpt-3.5-turbo")
      patches.add(patch)
      log.debug("LLM analysis: Switching to more accessible model")
    }

    return patches
  }

  private fun optimizeResourceUsage(inputs: Map<String, Any>): List<JsonElement> {
    val patches = mutableListOf<JsonElement>()

    // Reduce resource-intensive parameters
    inputs["temperature"]?.let { value ->
      if (value is Number && value.toDouble() > 1.0) {
        val patch = createReplacePatch("/temperature", 0.7)
        patches.add(patch)
        log.debug("LLM analysis: Reducing temperature to optimize resources")
      }
    }

    inputs["numCompletions"]?.let { value ->
      if (value is Number && value.toInt() > 1) {
        val patch = createReplacePatch("/numCompletions", 1)
        patches.add(patch)
        log.debug("LLM analysis: Reducing numCompletions to save resources")
      }
    }

    return patches
  }

  private fun createReplacePatch(path: String, value: Any): JsonElement {
    return buildJsonObject {
      put("op", "replace")
      put("path", path)
      when (value) {
        is String -> put("value", value)
        is Number -> put("value", value)
        is Boolean -> put("value", value)
        else -> put("value", value.toString())
      }
    }
  }

  private fun createRemovePatch(path: String): JsonElement {
    return buildJsonObject {
      put("op", "remove")
      put("path", path)
    }
  }
}