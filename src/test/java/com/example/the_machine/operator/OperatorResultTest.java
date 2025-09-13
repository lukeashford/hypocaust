package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.operator.result.OperatorResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OperatorResultTest {

  @Test
  void testSuccessFactory() {
    Map<String, Object> inputs = Map.of("param1", "value1");
    Map<String, Object> outputs = Map.of("result", "success");

    final var result = OperatorResult.success("TestOp", "1.0", inputs, outputs);

    assertTrue(result.isOk());
    assertEquals(OperatorResultCode.SUCCESS, result.getCode());
    assertEquals("Operation completed successfully", result.getMessage());
    assertEquals("TestOp", result.getOperatorName());
    assertEquals("1.0", result.getOperatorVersion());
    assertEquals(inputs, result.getNormalizedInputs());
    assertEquals(outputs, result.getOutputs());
    assertEquals(1, result.getAttempts());
  }

  @Test
  void testSuccessFactoryWithCustomMessage() {
    Map<String, Object> inputs = Map.of("param1", "value1");
    Map<String, Object> outputs = Map.of("result", "success");

    final var result = OperatorResult.success("TestOp", "1.0", "Custom success", inputs, outputs);

    assertTrue(result.isOk());
    assertEquals(OperatorResultCode.SUCCESS, result.getCode());
    assertEquals("Custom success", result.getMessage());
    assertEquals("TestOp", result.getOperatorName());
    assertEquals("1.0", result.getOperatorVersion());
  }

  @Test
  void testFailureFactory() {
    Map<String, Object> inputs = Map.of("param1", "value1");

    final var result = OperatorResult.failure("TestOp", "1.0", OperatorResultCode.VALIDATION_ERROR,
        "Parameter validation failed", inputs);

    assertFalse(result.isOk());
    assertEquals(OperatorResultCode.VALIDATION_ERROR, result.getCode());
    assertEquals("Parameter validation failed", result.getMessage());
    assertEquals("TestOp", result.getOperatorName());
    assertEquals("1.0", result.getOperatorVersion());
    assertEquals(inputs, result.getNormalizedInputs());
    assertTrue(result.getOutputs().isEmpty());
  }

  @Test
  void testValidationFailureFactory() {
    final var result = OperatorResult.validationFailure("TestOp", "1.0",
        "Required parameter 'name' is missing");

    assertFalse(result.isOk());
    assertEquals(OperatorResultCode.VALIDATION_ERROR, result.getCode());
    assertEquals("Required parameter 'name' is missing", result.getMessage());
    assertEquals("TestOp", result.getOperatorName());
    assertEquals("1.0", result.getOperatorVersion());
  }

  @Test
  void testWithAttempts() {
    final var original = OperatorResult.success("TestOp", "1.0", Map.of(), Map.of());
    final var updated = original.withAttempts(3);

    assertEquals(1, original.getAttempts());
    assertEquals(3, updated.getAttempts());

    // Ensure other fields are preserved
    assertEquals(original.isOk(), updated.isOk());
    assertEquals(original.getCode(), updated.getCode());
    assertEquals(original.getOperatorName(), updated.getOperatorName());
  }

  @Test
  void testWithMetrics() {
    final var original = OperatorResult.success("TestOp", "1.0", Map.of(), Map.of());
    Map<String, Object> additionalMetrics = Map.of("latency", 150L, "retries", 2);
    final var updated = original.withMetrics(additionalMetrics);

    assertTrue(original.getMetrics().isEmpty());
    assertEquals(150L, updated.getMetrics().get("latency"));
    assertEquals(2, updated.getMetrics().get("retries"));

    // Test merging with existing metrics
    Map<String, Object> existingMetrics = Map.of("cpu", 0.8);
    final var withExisting = OperatorResult.success("TestOp", "1.0", Map.of(), Map.of())
        .withMetrics(existingMetrics);

    final var merged = withExisting.withMetrics(additionalMetrics);
    assertEquals(0.8, merged.getMetrics().get("cpu"));
    assertEquals(150L, merged.getMetrics().get("latency"));
    assertEquals(2, merged.getMetrics().get("retries"));
  }

  @Test
  void testWithRemediationPatches() {
    final var original = OperatorResult.success("TestOp", "1.0", Map.of(), Map.of());
    final var objectMapper = new ObjectMapper();
    final var patch = objectMapper.createObjectNode().put("op", "replace").put("path", "/param1")
        .put("value", "newValue");
    final var patches = List.<com.fasterxml.jackson.databind.JsonNode>of(patch);

    final var updated = original.withRemediationPatches(patches);

    assertTrue(original.getRemediationPatches().isEmpty());
    assertEquals(1, updated.getRemediationPatches().size());
    assertEquals("replace", updated.getRemediationPatches().getFirst().get("op").asText());
  }

  @Test
  void testToString() {
    final var result = OperatorResult.success("TestOperator", "2.1", Map.of(), Map.of());
    final var toString = result.toString();

    assertTrue(toString.contains("ok=true"));
    assertTrue(toString.contains("code='SUCCESS'"));
    assertTrue(toString.contains("operator='TestOperator@2.1'"));
    assertTrue(toString.contains("attempts=1"));
    assertTrue(toString.contains("message='Operation completed successfully'"));
  }

  @Test
  void testNullHandling() {
    // Test with null inputs and outputs
    final var result = OperatorResult.success("TestOp", "1.0", null, null);

    assertNotNull(result.getNormalizedInputs());
    assertNotNull(result.getOutputs());
    assertTrue(result.getNormalizedInputs().isEmpty());
    assertTrue(result.getOutputs().isEmpty());
  }

  @Test
  void testCustomCodeViaFailureMethod() {
    final var result = OperatorResult.failure("CustomOp", "3.0",
        OperatorResultCode.EXECUTION_FAILED,
        "Custom operation completed", Map.of());

    assertFalse(result.isOk()); // failure method creates failed results
    assertEquals(OperatorResultCode.EXECUTION_FAILED, result.getCode());
    assertEquals("Custom operation completed", result.getMessage());
    assertEquals("CustomOp", result.getOperatorName());
    assertEquals("3.0", result.getOperatorVersion());
    assertEquals(1, result.getAttempts()); // default attempts
  }
}