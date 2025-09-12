package com.example.the_machine.operator

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.operator.result.OperatorResult
import com.example.the_machine.operator.result.OperatorResultCode
import com.example.the_machine.operator.result.ValidationResult
import com.example.the_machine.service.RunContext
import com.example.the_machine.service.RunPolicy
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class BaseOperatorTest {

  @MockK(relaxed = true)
  private lateinit var mockContext: RunContext

  @MockK(relaxed = true)
  private lateinit var mockToolSpec: ToolSpec

  @MockK(relaxed = true)
  private lateinit var mockHeuristicRemediator: BaseOperator.Remediator

  @MockK(relaxed = true)
  private lateinit var mockLLMRemediator: BaseOperator.Remediator

  private lateinit var testOperator: TestOperator

  @BeforeEach
  fun setUp() {
    // Setup mock remediators
    every { mockHeuristicRemediator.name } returns "HeuristicRemediator"
    every { mockLLMRemediator.name } returns "LLMRemediator"

    // Create TestOperator with mock remediators
    val remediators = listOf(mockHeuristicRemediator, mockLLMRemediator)
    testOperator = TestOperator(remediators)

    // Setup default policy
    val policy = RunPolicy(3, 10.0, 10, 100)
    every { mockContext.policy } returns policy

    // Setup checkBudgets to not throw by default (relaxed mocks handle this automatically)
    every { mockContext.checkBudgets() } just Runs
  }

  @Test
  fun testSuccessPath() {
    // Given
    val rawInputs = mapOf<String, Any>("param1" to "value1")
    val normalizedInputs = mapOf("param1" to "value1", "param2" to "defaultValue")
    val outputs = mapOf<String, Any>("result" to "success")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)
    testOperator.setSuccessResult(
      OperatorResult.success("TestOperator", "1.0.0", normalizedInputs, outputs)
    )

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertTrue(result.ok)
    assertEquals(outputs, result.outputs)
    assertEquals(normalizedInputs, result.normalizedInputs)
    assertEquals(1, result.attempts)
    assertTrue(result.metrics.containsKey("latencyMs"))
    verify { mockContext.checkBudgets() }
  }

  @Test
  fun testFailureWithRemediationThenSuccess() {
    // Given
    val rawInputs = mapOf<String, Any>("timeout" to 10)
    val normalizedInputs = mapOf<String, Any>("timeout" to 10)
    val outputs = mapOf<String, Any>("result" to "success after remediation")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)

    // Setup mock remediation patches - create a timeout adjustment patch
    val timeoutPatch = KotlinSerializationConfig.staticJson.parseToJsonElement(
      """
      {
        "op": "replace",
        "path": "/timeout",
        "value": 20
      }
    """
    )

    // Configure mock HeuristicRemediator to return patches for timeout error
    val timeoutException = RuntimeException("Operation timed out")
    every {
      mockHeuristicRemediator.remediate(
        mockContext, normalizedInputs, timeoutException,
        "timeout,backoff"
      )
    } returns listOf(timeoutPatch)
    // LLMRemediator won't be called since HeuristicRemediator succeeds, relaxed mocks handle this
    every {
      mockLLMRemediator.remediate(
        mockContext, normalizedInputs, timeoutException,
        "timeout,backoff"
      )
    } returns emptyList()

    // First attempt fails with timeout error
    testOperator.setFailureOnAttempt(1, timeoutException)
    testOperator.setSuccessResultForAttempt(
      2,
      OperatorResult.success("TestOperator", "1.0.0", normalizedInputs, outputs)
    )

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertTrue(result.ok)
    assertEquals(outputs, result.outputs)
    assertEquals(2, result.attempts)
    assertFalse(result.remediationPatches.isEmpty())
    assertTrue(result.metrics.containsKey("latencyMs"))

    // Verify remediation was applied (timeout should be doubled)
    assertEquals(2, testOperator.executionAttempts)
  }

  @Test
  fun testHittingMaxTriesPerOp() {
    // Given
    val rawInputs = mapOf<String, Any>("param1" to "value1")
    val normalizedInputs = mapOf<String, Any>("param1" to "value1")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)

    // All attempts fail
    val exception = RuntimeException("Persistent failure")
    testOperator.setAlwaysFailWith(exception)

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertFalse(result.ok)
    assertEquals(3, result.attempts) // Should match maxTriesPerOp
    assertEquals(OperatorResultCode.EXECUTION_FAILED, result.code)
    assertTrue(result.message.contains("Persistent failure"))
    assertTrue(result.metrics.containsKey("latencyMs"))
  }

  @Test
  fun testSecretRedaction() {
    // Given
    val secretValue = "super-secret-key-123"
    val rawInputs = mapOf<String, Any>("apiKey" to secretValue, "model" to "gpt-4")
    val normalizedInputs = mapOf<String, Any>("apiKey" to secretValue, "model" to "gpt-4")

    // Setup secret parameter
    val secretParamSpec = mockk<ParamSpec<*>>()
    every { secretParamSpec.name } returns "apiKey"
    every { secretParamSpec.secret } returns true

    val regularParamSpec = mockk<ParamSpec<*>>()
    every { regularParamSpec.name } returns "model"
    every { regularParamSpec.secret } returns false

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)
    val specs = mutableListOf<ParamSpec<*>>()
    specs.add(secretParamSpec)
    specs.add(regularParamSpec)
    every { mockToolSpec.inputs } returns specs

    // Fail with error message containing secret
    val exception = RuntimeException("Authentication failed with key: $secretValue")
    testOperator.setAlwaysFailWith(exception)

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertFalse(result.ok)
    assertFalse(result.message.contains(secretValue))
    assertTrue(result.message.contains("[REDACTED]"))
    assertTrue(result.message.contains("Authentication failed with key: [REDACTED]"))
  }

  @Test
  fun testValidationFailure() {
    // Given
    val rawInputs = mapOf<String, Any>("invalidParam" to "value")

    val validationResult = mockk<ValidationResult>()
    every { validationResult.ok } returns false
    every { validationResult.message } returns "Invalid parameter: invalidParam"

    every { mockToolSpec.validate(rawInputs) } returns validationResult
    testOperator.setMockToolSpec(mockToolSpec)

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertFalse(result.ok)
    assertEquals(OperatorResultCode.VALIDATION_ERROR, result.code)
    assertTrue(result.message.contains("Invalid parameter: invalidParam"))
    assertEquals(1, result.attempts)
  }

  @Test
  fun testOperatorVersionIsReturnedInResult() {
    // Given
    val rawInputs = mapOf<String, Any>("param" to "value")
    val normalizedInputs = mapOf<String, Any>("param" to "value")

    setupMockValidationAndDefaults(rawInputs, normalizedInputs)
    testOperator.setSuccessResult(OperatorResult.success())

    // When
    val result = testOperator.execute(mockContext, rawInputs)

    // Then
    assertTrue(result.ok)
    assertEquals("1.0.0", result.operatorVersion)
    assertEquals("TestOperator", result.operatorName)
  }

  private fun setupMockValidationAndDefaults(
    rawInputs: Map<String, Any>,
    normalizedInputs: Map<String, Any>
  ) {
    val validationResult = mockk<ValidationResult>()
    every { validationResult.ok } returns true

    every { mockToolSpec.validate(rawInputs) } returns validationResult
    every { mockToolSpec.applyDefaults(rawInputs) } returns normalizedInputs
    every { mockToolSpec.inputs } returns emptyList<ParamSpec<*>>()

    testOperator.setMockToolSpec(mockToolSpec)
  }

  /**
   * Test implementation of BaseOperator for testing purposes.
   */
  private class TestOperator(
    remediators: List<Remediator>
  ) : BaseOperator(remediators) {

    private var mockToolSpec: ToolSpec? = null
    private var successResult: OperatorResult? = null
    private var alwaysFailWith: Exception? = null
    private val failureOnAttempt = mutableMapOf<Int, Exception>()
    private val successResultForAttempt = mutableMapOf<Int, OperatorResult>()
    var executionAttempts = 0
      private set

    fun setMockToolSpec(mockToolSpec: ToolSpec) {
      this.mockToolSpec = mockToolSpec
    }

    fun setSuccessResult(successResult: OperatorResult) {
      this.successResult = successResult
    }

    fun setAlwaysFailWith(alwaysFailWith: Exception) {
      this.alwaysFailWith = alwaysFailWith
    }

    override fun spec(): ToolSpec {
      return mockToolSpec ?: throw IllegalStateException("Mock ToolSpec not set")
    }

    override fun doExecute(ctx: RunContext, inputs: Map<String, Any>): OperatorResult {
      executionAttempts++

      alwaysFailWith?.let { throw it }

      failureOnAttempt[executionAttempts]?.let { throw it }

      successResultForAttempt[executionAttempts]?.let { return it }

      return successResult ?: OperatorResult.success()
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