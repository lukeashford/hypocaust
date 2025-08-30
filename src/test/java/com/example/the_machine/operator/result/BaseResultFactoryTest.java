package com.example.the_machine.operator.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Test to verify the BaseResult factory helpers enforce the success/failure contract correctly.
 */
class BaseResultFactoryTest {

  @Test
  void testValidationResultSuccessEnforcesOkTrue() {
    System.out.println("[DEBUG_LOG] Testing ValidationResult.success() enforces ok=true");

    var result1 = ValidationResult.success();
    assertTrue(result1.isOk(), "ValidationResult.success() should set ok=true");
    assertEquals("Validation completed successfully", result1.getMessage());

    var result2 = ValidationResult.success("Custom success message");
    assertTrue(result2.isOk(), "ValidationResult.success(message) should set ok=true");
    assertEquals("Custom success message", result2.getMessage());

    System.out.println("[DEBUG_LOG] ValidationResult success methods correctly enforce ok=true");
  }

  @Test
  void testValidationResultErrorEnforcesOkFalse() {
    System.out.println("[DEBUG_LOG] Testing ValidationResult.error() enforces ok=false");

    var result = ValidationResult.error("Validation failed");
    assertFalse(result.isOk(), "ValidationResult.error() should set ok=false");
    assertEquals("Validation failed", result.getMessage());
    assertEquals("ERROR", result.getValidationType());

    System.out.println("[DEBUG_LOG] ValidationResult error methods correctly enforce ok=false");
  }

  @Test
  void testOperatorResultSuccessEnforcesOkTrue() {
    System.out.println("[DEBUG_LOG] Testing OperatorResult.success() enforces ok=true");

    var result1 = OperatorResult.success();
    assertTrue(result1.isOk(), "OperatorResult.success() should set ok=true");
    assertEquals("Operation completed successfully", result1.getMessage());

    var result2 = OperatorResult.success("Custom success message");
    assertTrue(result2.isOk(), "OperatorResult.success(message) should set ok=true");
    assertEquals("Custom success message", result2.getMessage());

    System.out.println("[DEBUG_LOG] OperatorResult success methods correctly enforce ok=true");
  }

  @Test
  void testOperatorResultFailureEnforcesOkFalse() {
    System.out.println("[DEBUG_LOG] Testing OperatorResult.failure() enforces ok=false");

    var result = OperatorResult.failure("TestOp", "1.0", OperatorResultCode.EXECUTION_FAILED,
        "Custom failure message", Map.of());
    assertFalse(result.isOk(), "OperatorResult.failure() should set ok=false");
    assertEquals("Custom failure message", result.getMessage());
    assertEquals(OperatorResultCode.EXECUTION_FAILED, result.getCode());

    System.out.println("[DEBUG_LOG] OperatorResult failure methods correctly enforce ok=false");
  }

  @Test
  void testSuccessFailureContractConsistency() {
    System.out.println(
        "[DEBUG_LOG] Testing success/failure contract consistency across subclasses");

    // All success methods should result in ok=true
    assertTrue(ValidationResult.success().isOk());
    assertTrue(ValidationResult.success("test").isOk());
    assertTrue(OperatorResult.success().isOk());
    assertTrue(OperatorResult.success("test").isOk());
    assertTrue(OperatorResult.success("op", "1.0", Map.of(), Map.of()).isOk());

    // All failure/error methods should result in ok=false
    assertFalse(ValidationResult.error("test").isOk());
    assertFalse(
        OperatorResult.failure("op", "1.0", OperatorResultCode.EXECUTION_FAILED, "test", Map.of())
            .isOk());
    assertFalse(OperatorResult.validationFailure("op", "1.0", "test").isOk());
    assertFalse(
        OperatorResult.failure("op", "1.0", OperatorResultCode.UNEXPECTED_ERROR, "test", Map.of())
            .isOk());

    System.out.println(
        "[DEBUG_LOG] Success/failure contract is consistently enforced across all factory methods");
  }
}