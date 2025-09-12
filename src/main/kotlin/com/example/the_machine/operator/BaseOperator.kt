package com.example.the_machine.operator

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.operator.result.OperatorResult
import com.example.the_machine.operator.result.OperatorResultCode
import com.example.the_machine.service.RunContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.time.Instant

/**
 * Base implementation of Operator that provides common lifecycle management including validation,
 * defaults, budget checks, timing, retry logic, and schema-aware remediation. Concrete operators
 * should extend this class and implement doExecute().
 */
abstract class BaseOperator(
  private val remediators: List<Remediator>
) : Operator {

  companion object {

    private val log = KotlinLogging.logger {}
  }

  /**
   * Returns the version of this operator implementation. Should follow semantic versioning (e.g.,
   * "1.0.0", "2.1.3").
   *
   * @return the operator version
   */
  protected abstract fun getVersion(): String

  override fun execute(ctx: RunContext, rawInputs: Map<String, Any>): OperatorResult {
    val startTime = Instant.now()
    val spec = spec()
    val operatorName = this::class.simpleName ?: "UnknownOperator"
    val operatorVersion = getVersion()

    log.debug("Starting execution of {} with inputs: {}", operatorName, rawInputs.keys)

    return try {
      // Step 1: Validate raw inputs against ToolSpec
      val validationResult = spec.validate(rawInputs)
      if (!validationResult.ok) {
        log.warn("Validation failed for {}: {}", operatorName, validationResult.message)
        return OperatorResult.validationFailure(
          operatorName, operatorVersion,
          validationResult.message
        )
      }

      // Step 2: Apply defaults
      val normalizedInputs = spec.applyDefaults(rawInputs)
      log.debug { "Applied defaults, normalized inputs: ${normalizedInputs.keys}" }

      // Step 3: Check budgets
      ctx.checkBudgets()

      // Step 4: Execute with retry logic
      executeWithRetries(ctx, normalizedInputs, operatorName, operatorVersion, startTime)

    } catch (e: Exception) {
      val latencyMs = calculateLatency(startTime)
      log.error(e) { "Unexpected error in $operatorName after ${latencyMs}ms" }

      // Redact secrets from error message
      val redactedMessage =
        redactSecrets(e.message ?: "Unknown error", rawInputs) ?: "Unknown error"

      OperatorResult.failure(
        operatorName, operatorVersion,
        OperatorResultCode.UNEXPECTED_ERROR,
        redactedMessage, HashMap(rawInputs)
      ).withMetrics(mapOf("latencyMs" to latencyMs))
    }
  }

  /**
   * Execute with retry and remediation logic using 0-based attempt indexing.
   */
  private fun executeWithRetries(
    ctx: RunContext,
    normalizedInputs: Map<String, Any>,
    operatorName: String,
    operatorVersion: String,
    startTime: Instant
  ): OperatorResult {
    val allPatches = mutableListOf<JsonElement>()
    val currentInputs = HashMap(normalizedInputs)
    val maxTries = ctx.policy.maxTriesPerOp

    var lastException: Exception? = null
    var lastErrorSignature: String? = null
    var attemptsForCurrentError = 0

    for (attempt in 0 until maxTries) {
      try {
        log.debug("Attempt {}/{} for {}", attempt, maxTries - 1, operatorName)

        val result = doExecute(ctx, currentInputs)

        // Success path
        val latencyMs = calculateLatency(startTime)
        log.debug(
          "Successfully executed {} in {}ms after {} attempts",
          operatorName, latencyMs, attempt + 1
        )

        return OperatorResult.success(
          operatorName, operatorVersion, normalizedInputs,
          result.outputs
        )
          .withMetrics(mapOf("latencyMs" to latencyMs))
          .withAttempts(attempt + 1)
          .withRemediationPatches(allPatches)

      } catch (e: Exception) {
        lastException = e
        val currentErrorSignature = createErrorSignature(e)
        log.warn(
          "Attempt {}/{} failed for {}: {}",
          attempt, maxTries - 1, operatorName, e.message
        )

        if (attempt < maxTries - 1) {
          // Check if error signature changed - if so, restart with first remediator
          if (currentErrorSignature != lastErrorSignature) {
            log.debug(
              "Error signature changed from '{}' to '{}', restarting with first remediator",
              lastErrorSignature, currentErrorSignature
            )
            attemptsForCurrentError = 0
            lastErrorSignature = currentErrorSignature
          }

          // Check if we've tried all remediators for this error
          if (attemptsForCurrentError >= remediators.size) {
            log.info(
              "Tried all {} remediators for error '{}', stopping early at attempt {}",
              remediators.size, currentErrorSignature, attempt
            )
            break
          }

          // Try remediation with current remediator
          val remediationPatches = getPatchFromRemediator(
            ctx, currentInputs, e, attemptsForCurrentError
          )
          if (remediationPatches.isEmpty()) {
            log.debug(
              "No remediation available from remediator {}, will retry with same inputs",
              attemptsForCurrentError
            )
          } else {
            applyPatches(currentInputs, remediationPatches) // Now modifies in-place
            allPatches.addAll(remediationPatches)
            log.debug(
              "Applied {} remediation patches from remediator {} for {}",
              remediationPatches.size, attemptsForCurrentError, operatorName
            )
          }

          attemptsForCurrentError++
        }
      }
    }

    // All retries exhausted or early termination - create failure result
    val latencyMs = calculateLatency(startTime)
    val redactedMessage = lastException?.message?.let { redactSecrets(it, normalizedInputs) } ?: ""

    return OperatorResult.failure(
      operatorName, operatorVersion,
      OperatorResultCode.EXECUTION_FAILED, redactedMessage, normalizedInputs
    )
      .withMetrics(mapOf("latencyMs" to latencyMs))
      .withAttempts(maxTries)
      .withRemediationPatches(allPatches)
  }

  /**
   * Gets patch from a specific remediator using direct indexing.
   */
  private fun getPatchFromRemediator(
    ctx: RunContext,
    normalizedInputs: Map<String, Any>,
    exception: Exception,
    remediatorIndex: Int
  ): List<JsonElement> {
    if (remediators.isEmpty() || remediatorIndex >= remediators.size) {
      return emptyList()
    }

    val remediator = remediators[remediatorIndex]
    val hints = remediationHints()

    return try {
      val patches = remediator.remediate(ctx, normalizedInputs, exception, hints)
      log.debug(
        "Remediator {} ({}) generated {} patches",
        remediatorIndex, remediator.name, patches.size
      )
      patches
    } catch (remediationError: Exception) {
      log.warn(
        "Remediator {} ({}) failed: {}",
        remediatorIndex, remediator.name, remediationError.message
      )
      emptyList()
    }
  }

  /**
   * Creates a signature for an error to detect if we're getting the same error repeatedly.
   */
  private fun createErrorSignature(e: Exception): String {
    // Use exception class name and first 100 chars of message for signature
    val message = e.message
    val truncatedMessage = when {
      message != null && message.length > 100 -> message.take(100)
      message != null -> message
      else -> ""
    }
    return "${e::class.simpleName}:$truncatedMessage"
  }

  /**
   * Applies simple JSON patches to input map in-place. Supports "replace", "add", and "remove"
   * operations.
   */
  private fun applyPatches(inputs: MutableMap<String, Any>, patches: List<JsonElement>) {
    try {
      // Apply each patch directly to the input map
      for (patchNode in patches) {
        val patchObj = patchNode.jsonObject
        if (!patchObj.containsKey("op") || !patchObj.containsKey("path")) {
          log.warn("Invalid patch format, skipping: {}", patchNode)
          continue
        }

        val operation = patchObj["op"]?.jsonPrimitive?.content
        val path = patchObj["path"]?.jsonPrimitive?.content

        if (operation == null || path == null) {
          log.warn("Invalid patch operation or path, skipping: {}", patchNode)
          continue
        }

        // Simple path handling - only support root level fields for now
        if (!path.startsWith("/") || path.indexOf("/", 1) != -1) {
          log.warn("Only root-level paths supported, skipping: {}", path)
          continue
        }

        val fieldName = path.substring(1) // Remove leading "/"

        when (operation) {
          "replace", "add" -> {
            val valueElement = patchObj["value"]
            if (valueElement != null) {
              val value =
                KotlinSerializationConfig.staticJson.decodeFromJsonElement<Any>(valueElement)
              inputs[fieldName] = value
              log.debug("Applied {} patch: {} = {}", operation, fieldName, value)
            }
          }

          "remove" -> {
            inputs.remove(fieldName)
            log.debug("Applied remove patch: {}", fieldName)
          }

          else -> {
            log.warn("Unsupported patch operation: {}", operation)
          }
        }
      }
    } catch (e: Exception) {
      log.warn("Failed to apply remediation patches: {}", e.message)
      // Input map remains unchanged if patching fails
    }
  }

  /**
   * Redacts secrets from error messages based on parameter specifications.
   */
  private fun redactSecrets(message: String?, inputs: Map<String, Any>): String? {
    if (message == null) {
      return null
    }

    return spec().inputs
      .filter { param -> param.secret && inputs.containsKey(param.name) }
      .mapNotNull { param -> inputs[param.name] }
      .filter { value -> value.toString().isNotEmpty() }
      .map { it.toString() }
      .fold(message) { msg, secretValue -> msg.replace(secretValue, "[REDACTED]") }
  }

  /**
   * Calculates latency from start time to now.
   */
  private fun calculateLatency(startTime: Instant): Long =
    Instant.now().toEpochMilli() - startTime.toEpochMilli()

  /**
   * Concrete operators implement their specific logic here. This method will be called within the
   * retry loop managed by execute().
   *
   * @param ctx the run context
   * @param inputs the validated and normalized inputs (may include remediation patches)
   * @return the operator result with outputs
   * @throws Exception if the operation fails
   */
  @Throws(Exception::class)
  protected abstract fun doExecute(ctx: RunContext, inputs: Map<String, Any>): OperatorResult

  /**
   * Override this method to provide remediation hints to remediators. Hints can guide the
   * remediation strategy.
   *
   * @return remediation hints as a string, or null if no specific hints
   */
  protected open fun remediationHints(): String? = null

  /**
   * Interface that remediators must implement for providing patches.
   * This is a placeholder interface - the actual Remediator interface should be defined elsewhere.
   */
  interface Remediator {

    val name: String
    fun remediate(
      ctx: RunContext,
      inputs: Map<String, Any>,
      exception: Exception,
      hints: String?
    ): List<JsonElement>
  }
}