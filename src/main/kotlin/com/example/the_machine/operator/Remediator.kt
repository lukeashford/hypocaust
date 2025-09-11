package com.example.the_machine.operator

import com.example.the_machine.service.RunContext
import com.fasterxml.jackson.databind.JsonNode

/**
 * Interface for auto-remediation of operator failures. Implementations can propose adjustments to
 * operator inputs based on the error context, constrained to fields marked as adjustable and within
 * schema bounds.
 */
interface Remediator {

  /**
   * Attempts to remediate a failed operator execution by proposing input adjustments.
   *
   * @param ctx the run context
   * @param normalizedInputs the current normalized inputs that caused the failure
   * @param exception the exception that occurred
   * @param remediationHints optional hints from the operator about remediation strategy
   * @return a list of JSON patches to apply to the inputs, or empty list if no remediation possible
   */
  fun remediate(
    ctx: RunContext,
    normalizedInputs: Map<String, Any>,
    exception: Exception,
    remediationHints: String?
  ): List<JsonNode>

  /**
   * Returns the name/type of this remediator for logging and debugging.
   *
   * @return the remediator name
   */
  fun getName(): String = javaClass.simpleName
}