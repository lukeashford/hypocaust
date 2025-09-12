package com.example.the_machine.operator

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.operator.result.OperatorResult
import com.example.the_machine.operator.result.ValidationResult
import com.example.the_machine.service.RunContext
import com.example.the_machine.service.RunPolicy
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration test to verify the sequential remediation strategy: try heuristic first, if that
 * fails try LLM, if both fail return empty.
 */
class SequentialRemediationTest {

  private val mockContext = mockk<RunContext>()
  private val mockToolSpec = mockk<ToolSpec>()
  private val mockHeuristicRemediator = mockk<BaseOperator.Remediator>()
  private val mockLLMRemediator = mockk<BaseOperator.Remediator>()

  private lateinit var testOperator: TestOperator

  @BeforeEach
  fun setUp() {
    // Setup mock remediators with correct names for sequential logic
    every { mockHeuristicRemediator.name } returns "HeuristicRemediator"
    every { mockLLMRemediator.name } returns "LLMRemediator"

    // Create TestOperator with mock remediators
    val remediators = listOf(mockHeuristicRemediator, mockLLMRemediator)
    testOperator = TestOperator(remediators)

    // Setup default policy
    val policy = RunPolicy(3, 10.0, 10, 100)
    every { mockContext.policy } returns policy
    every { mockContext.checkBudgets() } just Runs
  }

  @Test
  fun testHeuristicSucceeds_LLMNotCalled() {
    // Given
    val rawInputs = mapOf<String, Any>("timeout" to 10)
    val normalizedInputs = mapOf<String, Any>("timeout" to 10)
    val exception = RuntimeException("Operation timed out")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)

    // Create a timeout adjustment patch
    val timeoutPatch = KotlinSerializationConfig.staticJson.parseToJsonElement(
      """
      {
        "op": "replace",
        "path": "/timeout",
        "value": 20
      }
    """
    )

    // Heuristic succeeds
    every {
      mockHeuristicRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    } returns listOf(timeoutPatch)

    // Set up failure then success
    testOperator.setFailureOnAttempt(1, exception)
    testOperator.setSuccessResultForAttempt(2, OperatorResult.success())

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertTrue(result.ok)
    assertEquals(2, result.attempts)
    assertFalse(result.remediationPatches.isEmpty())

    // Verify heuristic was called but LLM was not
    verify(exactly = 1) {
      mockHeuristicRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    }
    verify(exactly = 0) {
      mockLLMRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    }
  }

  @Test
  fun testHeuristicFails_LLMSucceeds() {
    // Given
    val rawInputs = mapOf<String, Any>("model" to "gpt-4")
    val normalizedInputs = mapOf<String, Any>("model" to "gpt-4")
    val exception = RuntimeException("Model unavailable")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)

    // Create a model switch patch
    val modelPatch = KotlinSerializationConfig.staticJson.parseToJsonElement(
      """
      {
        "op": "replace",
        "path": "/model",
        "value": "gpt-3.5-turbo"
      }
    """
    )

    // Heuristic fails (returns empty), LLM succeeds
    every {
      mockHeuristicRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    } returns emptyList() // Empty - no patches

    every {
      mockLLMRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    } returns listOf(modelPatch)

    // Set up failure, then failure again (so remediation happens), then success
    testOperator.setFailureOnAttempt(1, exception)
    testOperator.setFailureOnAttempt(2, exception)
    testOperator.setSuccessResultForAttempt(3, OperatorResult.success())

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertTrue(result.ok)
    assertEquals(3, result.attempts)
    assertFalse(result.remediationPatches.isEmpty())

    // Verify both were called in sequence
    verify(exactly = 1) {
      mockHeuristicRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    }
    verify(exactly = 1) {
      mockLLMRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    }
  }

  @Test
  fun testBothRemediatorsFail_NoPatches() {
    // Given
    val rawInputs = mapOf<String, Any>("param" to "value")
    val normalizedInputs = mapOf<String, Any>("param" to "value")
    val exception = RuntimeException("Unknown error")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)

    // Both remediators return empty
    every {
      mockHeuristicRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    } returns emptyList()

    every {
      mockLLMRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    } returns emptyList()

    // Set up persistent failure
    testOperator.alwaysFailWith = exception

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertFalse(result.ok)
    assertEquals(3, result.attempts) // Max tries
    assertTrue(result.remediationPatches.isEmpty()) // No patches applied

    // Verify both were called once (new algorithm tries all remediators once then stops early)
    verify(exactly = 1) {
      mockHeuristicRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    }
    verify(exactly = 1) {
      mockLLMRemediator.remediate(
        mockContext,
        any(),
        exception,
        "timeout,backoff"
      )
    }
  }

  private fun setupMockValidationAndDefaults(
    rawInputs: Map<String, Any>,
    normalizedInputs: Map<String, Any>
  ) {
    val validationResult = mockk<ValidationResult>()
    every { validationResult.ok } returns true

    every { mockToolSpec.validate(rawInputs) } returns validationResult
    every { mockToolSpec.applyDefaults(rawInputs) } returns normalizedInputs
    every { mockToolSpec.inputs } returns emptyList()

    testOperator.mockToolSpec = mockToolSpec
  }

  /**
   * Test implementation of BaseOperator for testing purposes.
   */
  private class TestOperator(
    remediators: List<Remediator>
  ) : BaseOperator(remediators) {

    var mockToolSpec: ToolSpec? = null
    var alwaysFailWith: Exception? = null
    private val failureOnAttempt = mutableMapOf<Int, Exception>()
    private val successResultForAttempt = mutableMapOf<Int, OperatorResult>()
    private var executionAttempts = 0

    override fun spec(): ToolSpec = mockToolSpec!!

    override fun doExecute(ctx: RunContext, inputs: Map<String, Any>): OperatorResult {
      executionAttempts++

      alwaysFailWith?.let { throw it }

      failureOnAttempt[executionAttempts]?.let { throw it }

      successResultForAttempt[executionAttempts]?.let { return it }

      return OperatorResult.success()
    }

    override fun getVersion(): String = "1.0.0"

    override fun remediationHints(): String = "timeout,backoff"

    fun setFailureOnAttempt(attempt: Int, exception: Exception) {
      failureOnAttempt[attempt] = exception
    }

    fun setSuccessResultForAttempt(attempt: Int, result: OperatorResult) {
      successResultForAttempt[attempt] = result
    }
  }
}