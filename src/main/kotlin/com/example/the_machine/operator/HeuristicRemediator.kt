package com.example.the_machine.operator

import com.example.the_machine.service.RunContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Heuristic-based remediator that applies common remediation patterns: - Timeout/backoff
 * adjustments - Clamp out-of-range values to min/max bounds - Swap model values within enum
 * constraints
 */
@Component
@Order(0)
class HeuristicRemediator(
  private val objectMapper: ObjectMapper
) : Remediator {

  companion object {

    private val log = KotlinLogging.logger {}
  }

  override fun remediate(
    ctx: RunContext,
    normalizedInputs: Map<String, Any>,
    exception: Exception,
    remediationHints: String?
  ): List<JsonNode> {
    val patches = mutableListOf<JsonNode>()
    val exceptionMessage = exception.message?.lowercase() ?: ""

    log.debug("HeuristicRemediator attempting remediation for exception: {}", exception.message)

    // Timeout-related remediation
    if (exceptionMessage.contains("timeout") || exceptionMessage.contains("timed out")) {
      patches.addAll(adjustTimeouts(normalizedInputs))
    }

    // Rate limit remediation
    if (exceptionMessage.contains("rate limit") || exceptionMessage.contains("429")) {
      patches.addAll(addBackoff(normalizedInputs))
    }

    // Range/validation error remediation
    if (exceptionMessage.contains("out of range") || exceptionMessage.contains("invalid value")) {
      patches.addAll(clampValues(normalizedInputs))
    }

    // Model availability remediation
    if (exceptionMessage.contains("model") && exceptionMessage.contains("unavailable")) {
      patches.addAll(switchModel(normalizedInputs))
    }

    log.debug("HeuristicRemediator generated {} patches", patches.size)
    return patches
  }

  private fun adjustTimeouts(inputs: Map<String, Any>): List<JsonNode> {
    val patches = mutableListOf<JsonNode>()

    inputs.forEach { (key, value) ->
      if (key.lowercase().contains("timeout") && value is Number) {
        val currentTimeout = value.toInt()
        val newTimeout = minOf(currentTimeout * 2, 300) // Cap at 5 minutes

        val patch = createReplacePatch("/$key", newTimeout)
        patches.add(patch)
        log.debug("Adjusting timeout {} from {} to {}", key, currentTimeout, newTimeout)
      }
    }

    return patches
  }

  private fun addBackoff(inputs: Map<String, Any>): List<JsonNode> {
    val patches = mutableListOf<JsonNode>()

    // Add or increase retry delay
    if (!inputs.containsKey("retryDelayMs")) {
      val patch = objectMapper.createObjectNode().apply {
        put("op", "add")
        put("path", "/retryDelayMs")
        set<JsonNode>("value", objectMapper.valueToTree(1000))
      }
      patches.add(patch)
      log.debug("Adding retryDelayMs: 1000")
    } else if (inputs["retryDelayMs"] is Number) {
      val currentDelay = (inputs["retryDelayMs"] as Number).toInt()
      val newDelay = minOf(currentDelay * 2, 30000) // Cap at 30 seconds

      val patch = createReplacePatch("/retryDelayMs", newDelay)
      patches.add(patch)
      log.debug("Increasing retryDelayMs from {} to {}", currentDelay, newDelay)
    }

    return patches
  }

  private fun clampValues(inputs: Map<String, Any>): List<JsonNode> {
    val patches = mutableListOf<JsonNode>()

    inputs.forEach { (key, value) ->
      if (value is Number) {
        val numValue = value.toDouble()
        val clampedValue: Double? = when {
          key == "temperature" -> numValue.coerceIn(0.0, 2.0)
          key == "maxTokens" -> numValue.toInt().coerceIn(1, 4096).toDouble()
          key.contains("limit") || key.contains("max") -> maxOf(1.0, numValue)
          else -> null
        }

        if (clampedValue != null && clampedValue != numValue) {
          val patch = createReplacePatch("/$key", clampedValue)
          patches.add(patch)
          log.debug("Clamping {} from {} to {}", key, numValue, clampedValue)
        }
      }
    }

    return patches
  }

  private fun switchModel(inputs: Map<String, Any>): List<JsonNode> {
    val patches = mutableListOf<JsonNode>()

    val currentModel = inputs["model"] as? String
    if (currentModel != null) {
      val fallbackModel = getFallbackModel(currentModel)

      if (fallbackModel != null && fallbackModel != currentModel) {
        val patch = createReplacePatch("/model", fallbackModel)
        patches.add(patch)
        log.debug("Switching model from {} to {}", currentModel, fallbackModel)
      }
    }

    return patches
  }

  private fun getFallbackModel(currentModel: String): String? {
    // Simple fallback mapping - in real implementation this would be more sophisticated
    return when (currentModel.lowercase()) {
      "gpt-4" -> "gpt-3.5-turbo"
      "gpt-4-turbo" -> "gpt-4"
      "claude-3-opus" -> "claude-3-sonnet"
      "claude-3-sonnet" -> "claude-3-haiku"
      else -> null
    }
  }

  private fun createReplacePatch(path: String, value: Any): ObjectNode {
    return objectMapper.createObjectNode().apply {
      put("op", "replace")
      put("path", path)
      set<JsonNode>("value", objectMapper.valueToTree(value))
    }
  }
}