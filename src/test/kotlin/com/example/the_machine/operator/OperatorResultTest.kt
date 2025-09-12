package com.example.the_machine.operator

import com.example.the_machine.common.KotlinSerializationConfig
import com.example.the_machine.operator.result.OperatorResult
import com.example.the_machine.operator.result.OperatorResultCode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class OperatorResultTest {

  @Test
  fun testSuccessFactory() {
    val inputs = mapOf("param1" to "value1")
    val outputs = mapOf("result" to "success")

    val result = OperatorResult.success("TestOp", "1.0", inputs, outputs)

    assertTrue(result.ok)
    assertEquals(OperatorResultCode.SUCCESS, result.code)
    assertEquals("Operation completed successfully", result.message)
    assertEquals("TestOp", result.operatorName)
    assertEquals("1.0", result.operatorVersion)
    assertEquals(inputs, result.normalizedInputs)
    assertEquals(outputs, result.outputs)
    assertEquals(1, result.attempts)
  }

  @Test
  fun testSuccessFactoryWithCustomMessage() {
    val inputs = mapOf("param1" to "value1")
    val outputs = mapOf("result" to "success")

    val result = OperatorResult.success("TestOp", "1.0", "Custom success", inputs, outputs)

    assertTrue(result.ok)
    assertEquals(OperatorResultCode.SUCCESS, result.code)
    assertEquals("Custom success", result.message)
    assertEquals("TestOp", result.operatorName)
    assertEquals("1.0", result.operatorVersion)
  }

  @Test
  fun testFailureFactory() {
    val inputs = mapOf("param1" to "value1")

    val result = OperatorResult.failure(
      "TestOp", "1.0", OperatorResultCode.VALIDATION_ERROR,
      "Parameter validation failed", inputs
    )

    assertFalse(result.ok)
    assertEquals(OperatorResultCode.VALIDATION_ERROR, result.code)
    assertEquals("Parameter validation failed", result.message)
    assertEquals("TestOp", result.operatorName)
    assertEquals("1.0", result.operatorVersion)
    assertEquals(inputs, result.normalizedInputs)
    assertTrue(result.outputs.isEmpty())
  }

  @Test
  fun testValidationFailureFactory() {
    val result = OperatorResult.validationFailure(
      "TestOp", "1.0",
      "Required parameter 'name' is missing"
    )

    assertFalse(result.ok)
    assertEquals(OperatorResultCode.VALIDATION_ERROR, result.code)
    assertEquals("Required parameter 'name' is missing", result.message)
    assertEquals("TestOp", result.operatorName)
    assertEquals("1.0", result.operatorVersion)
  }

  @Test
  fun testWithAttempts() {
    val original = OperatorResult.success("TestOp", "1.0", emptyMap(), emptyMap())
    val updated = original.withAttempts(3)

    assertEquals(1, original.attempts)
    assertEquals(3, updated.attempts)

    // Ensure other fields are preserved
    assertEquals(original.ok, updated.ok)
    assertEquals(original.code, updated.code)
    assertEquals(original.operatorName, updated.operatorName)
  }

  @Test
  fun testWithMetrics() {
    val original = OperatorResult.success("TestOp", "1.0", emptyMap(), emptyMap())
    val additionalMetrics = mapOf("latency" to 150L, "retries" to 2)
    val updated = original.withMetrics(additionalMetrics)

    assertTrue(original.metrics.isEmpty())
    assertEquals(150L, updated.metrics["latency"])
    assertEquals(2L, updated.metrics["retries"])

    // Test merging with existing metrics
    val existingMetrics = mapOf("cpu" to 0.8)
    val withExisting = OperatorResult.success("TestOp", "1.0", emptyMap(), emptyMap())
      .withMetrics(existingMetrics)

    val merged = withExisting.withMetrics(additionalMetrics)
    assertEquals(0.8, merged.metrics["cpu"])
    assertEquals(150L, merged.metrics["latency"])
    assertEquals(2L, merged.metrics["retries"])
  }

  @Test
  fun testWithRemediationPatches() {
    val original = OperatorResult.success("TestOp", "1.0", emptyMap(), emptyMap())
    val patch = KotlinSerializationConfig.staticJson.parseToJsonElement(
      """
      {"op": "replace", "path": "/param1", "value": "newValue"}
    """
    )
    val patches = listOf<JsonElement>(patch)

    val updated = original.withRemediationPatches(patches)

    assertTrue(original.remediationPatches.isEmpty())
    assertEquals(1, updated.remediationPatches.size)
    assertEquals(
      "replace",
      updated.remediationPatches.first().jsonObject["op"]!!.jsonPrimitive.content
    )
  }

  @Test
  fun testToString() {
    val result = OperatorResult.success("TestOperator", "2.1", emptyMap(), emptyMap())
    val toString = result.toString()

    assertTrue(toString.contains("ok=true"))
    assertTrue(toString.contains("code=SUCCESS"))
    assertTrue(toString.contains("operatorName='TestOperator'"))
    assertTrue(toString.contains("operatorVersion='2.1'"))
    assertTrue(toString.contains("attempts=1"))
    assertTrue(toString.contains("message='Operation completed successfully'"))
  }

  @Test
  fun testNullHandling() {
    // Test with empty inputs and outputs (Kotlin doesn't accept null, but behavior should be same)
    val result = OperatorResult.success("TestOp", "1.0", emptyMap(), emptyMap())

    assertNotNull(result.normalizedInputs)
    assertNotNull(result.outputs)
    assertTrue(result.normalizedInputs.isEmpty())
    assertTrue(result.outputs.isEmpty())
  }

  @Test
  fun testCustomCodeViaFailureMethod() {
    val result = OperatorResult.failure(
      "CustomOp", "3.0", OperatorResultCode.EXECUTION_FAILED,
      "Custom operation completed", emptyMap()
    )

    assertFalse(result.ok) // failure method creates failed results
    assertEquals(OperatorResultCode.EXECUTION_FAILED, result.code)
    assertEquals("Custom operation completed", result.message)
    assertEquals("CustomOp", result.operatorName)
    assertEquals("3.0", result.operatorVersion)
    assertEquals(1, result.attempts) // default attempts
  }
}