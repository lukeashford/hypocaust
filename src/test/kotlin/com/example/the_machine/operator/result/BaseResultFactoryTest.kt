package com.example.the_machine.operator.result

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Test to verify the BaseResult factory helpers enforce the success/failure contract correctly.
 */
class BaseResultFactoryTest {

  @Test
  fun testValidationResultSuccessEnforcesOkTrue() {
    println("[DEBUG_LOG] Testing ValidationResult.success() enforces ok=true")

    val result1 = ValidationResult.success()
    assertTrue(result1.ok, "ValidationResult.success() should set ok=true")
    assertEquals("Validation completed successfully", result1.message)

    val result2 = ValidationResult.success("Custom success message")
    assertTrue(result2.ok, "ValidationResult.success(message) should set ok=true")
    assertEquals("Custom success message", result2.message)

    println("[DEBUG_LOG] ValidationResult success methods correctly enforce ok=true")
  }

  @Test
  fun testValidationResultErrorEnforcesOkFalse() {
    println("[DEBUG_LOG] Testing ValidationResult.error() enforces ok=false")

    val result = ValidationResult.error("Validation failed")
    assertFalse(result.ok, "ValidationResult.error() should set ok=false")
    assertEquals("Validation failed", result.message)
    assertEquals("ERROR", result.validationType)

    println("[DEBUG_LOG] ValidationResult error methods correctly enforce ok=false")
  }

  @Test
  fun testOperatorResultSuccessEnforcesOkTrue() {
    println("[DEBUG_LOG] Testing OperatorResult.success() enforces ok=true")

    val result1 = OperatorResult.success()
    assertTrue(result1.ok, "OperatorResult.success() should set ok=true")
    assertEquals("Operation completed successfully", result1.message)

    val result2 = OperatorResult.success("Custom success message")
    assertTrue(result2.ok, "OperatorResult.success(message) should set ok=true")
    assertEquals("Custom success message", result2.message)

    println("[DEBUG_LOG] OperatorResult success methods correctly enforce ok=true")
  }

  @Test
  fun testOperatorResultFailureEnforcesOkFalse() {
    println("[DEBUG_LOG] Testing OperatorResult.failure() enforces ok=false")

    val result = OperatorResult.failure(
      "TestOp",
      "1.0",
      OperatorResultCode.EXECUTION_FAILED,
      "Custom failure message",
      emptyMap()
    )
    assertFalse(result.ok, "OperatorResult.failure() should set ok=false")
    assertEquals("Custom failure message", result.message)
    assertEquals(OperatorResultCode.EXECUTION_FAILED, result.code)

    println("[DEBUG_LOG] OperatorResult failure methods correctly enforce ok=false")
  }

  @Test
  fun testSuccessFailureContractConsistency() {
    println("[DEBUG_LOG] Testing success/failure contract consistency across subclasses")

    // All success methods should result in ok=true
    assertTrue(ValidationResult.success().ok)
    assertTrue(ValidationResult.success("test").ok)
    assertTrue(OperatorResult.success().ok)
    assertTrue(OperatorResult.success("test").ok)
    assertTrue(OperatorResult.success("op", "1.0", emptyMap(), emptyMap()).ok)

    // All failure/error methods should result in ok=false
    assertFalse(ValidationResult.error("test").ok)
    assertFalse(
      OperatorResult.failure("op", "1.0", OperatorResultCode.EXECUTION_FAILED, "test", emptyMap())
        .ok
    )
    assertFalse(OperatorResult.validationFailure("op", "1.0", "test").ok)
    assertFalse(
      OperatorResult.failure("op", "1.0", OperatorResultCode.UNEXPECTED_ERROR, "test", emptyMap())
        .ok
    )

    println("[DEBUG_LOG] Success/failure contract is consistently enforced across all factory methods")
  }
}