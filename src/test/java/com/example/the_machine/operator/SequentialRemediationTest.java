package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.operator.result.ValidationResult;
import com.example.the_machine.service.RunContext;
import com.example.the_machine.service.RunPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration test to verify the sequential remediation strategy: try heuristic first, if that
 * fails try LLM, if both fail return empty.
 */
@ExtendWith(MockitoExtension.class)
class SequentialRemediationTest {

  @Mock
  private RunContext mockContext;

  @Mock
  private ToolSpec mockToolSpec;

  @Mock
  private Remediator mockHeuristicRemediator;

  @Mock
  private Remediator mockLLMRemediator;

  private ObjectMapper objectMapper;
  private TestOperator testOperator;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    // Setup mock remediators with correct names for sequential logic
    lenient().when(mockHeuristicRemediator.getName()).thenReturn("HeuristicRemediator");
    lenient().when(mockLLMRemediator.getName()).thenReturn("LLMRemediator");

    // Create TestOperator with mock remediators
    final var remediators = List.of(mockHeuristicRemediator, mockLLMRemediator);
    testOperator = new TestOperator(objectMapper, remediators);

    // Setup default policy
    final var policy = new RunPolicy(3, 10.0, 10, 100);
    lenient().when(mockContext.policy()).thenReturn(policy);
    lenient().doNothing().when(mockContext).checkBudgets();
  }

  @Test
  void testHeuristicSucceeds_LLMNotCalled() {
    // Given
    final var rawInputs = Map.<String, Object>of("timeout", 10);
    final var normalizedInputs = Map.<String, Object>of("timeout", 10);
    final var exception = new RuntimeException("Operation timed out");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);

    // Create a timeout adjustment patch
    final var timeoutPatch = objectMapper.createObjectNode();
    timeoutPatch.put("op", "replace");
    timeoutPatch.put("path", "/timeout");
    timeoutPatch.set("value", objectMapper.valueToTree(20));

    // Heuristic succeeds
    when(mockHeuristicRemediator.remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff")))
        .thenReturn(List.of(timeoutPatch));

    // Set up failure then success
    testOperator.setFailureOnAttempt(1, exception);
    testOperator.setSuccessResultForAttempt(2, OperatorResult.success());

    // When
    final var result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertTrue(result.isOk());
    assertEquals(2, result.getAttempts());
    assertFalse(result.getRemediationPatches().isEmpty());

    // Verify heuristic was called but LLM was not
    verify(mockHeuristicRemediator).remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff"));
    verify(mockLLMRemediator, never()).remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff"));
  }

  @Test
  void testHeuristicFails_LLMSucceeds() {
    // Given
    final var rawInputs = Map.<String, Object>of("model", "gpt-4");
    final var normalizedInputs = Map.<String, Object>of("model", "gpt-4");
    final var exception = new RuntimeException("Model unavailable");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);

    // Create a model switch patch
    final var modelPatch = objectMapper.createObjectNode();
    modelPatch.put("op", "replace");
    modelPatch.put("path", "/model");
    modelPatch.set("value", objectMapper.valueToTree("gpt-3.5-turbo"));

    // Heuristic fails (returns empty), LLM succeeds
    when(mockHeuristicRemediator.remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff")))
        .thenReturn(List.of()); // Empty - no patches
    when(mockLLMRemediator.remediate(eq(mockContext), ArgumentMatchers.any(),
        eq(exception), eq("timeout,backoff")))
        .thenReturn(List.of(modelPatch));

    // Set up failure, then failure again (so remediation happens), then success
    testOperator.setFailureOnAttempt(1, exception);
    testOperator.setFailureOnAttempt(2, exception);
    testOperator.setSuccessResultForAttempt(3, OperatorResult.success());

    // When
    final var result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertTrue(result.isOk());
    assertEquals(3, result.getAttempts());
    assertFalse(result.getRemediationPatches().isEmpty());

    // Verify both were called in sequence
    verify(mockHeuristicRemediator).remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff"));
    verify(mockLLMRemediator).remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff"));
  }

  @Test
  void testBothRemediatorsFail_NoPatches() {
    // Given
    final var rawInputs = Map.<String, Object>of("param", "value");
    final var normalizedInputs = Map.<String, Object>of("param", "value");
    final var exception = new RuntimeException("Unknown error");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);

    // Both remediators return empty
    when(mockHeuristicRemediator.remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception),
        eq("timeout,backoff")))
        .thenReturn(List.of());
    when(mockLLMRemediator.remediate(eq(mockContext), ArgumentMatchers.any(),
        eq(exception), eq("timeout,backoff")))
        .thenReturn(List.of());

    // Set up persistent failure
    testOperator.setAlwaysFailWith(exception);

    // When
    final var result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertFalse(result.isOk());
    assertEquals(3, result.getAttempts()); // Max tries
    assertTrue(result.getRemediationPatches().isEmpty()); // No patches applied

    // Verify both were called once (new algorithm tries all remediators once then stops early)
    verify(mockHeuristicRemediator, org.mockito.Mockito.times(1)).remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception), eq("timeout,backoff"));
    verify(mockLLMRemediator, org.mockito.Mockito.times(1)).remediate(eq(mockContext),
        ArgumentMatchers.any(), eq(exception), eq("timeout,backoff"));
  }

  private void setupMockValidationAndDefaults(Map<String, Object> rawInputs,
      Map<String, Object> normalizedInputs) {
    final var validationResult = mock(ValidationResult.class);
    lenient().when(validationResult.isOk()).thenReturn(true);

    lenient().when(mockToolSpec.validate(rawInputs)).thenReturn(validationResult);
    lenient().when(mockToolSpec.applyDefaults(rawInputs)).thenReturn(normalizedInputs);
    lenient().when(mockToolSpec.getInputs()).thenReturn(List.of());

    testOperator.setMockToolSpec(mockToolSpec);
  }

  /**
   * Test implementation of BaseOperator for testing purposes.
   */
  private static class TestOperator extends BaseOperator {

    @Setter
    private ToolSpec mockToolSpec;
    @Setter
    private Exception alwaysFailWith;
    private final Map<Integer, Exception> failureOnAttempt = new HashMap<>();
    private final Map<Integer, OperatorResult> successResultForAttempt = new HashMap<>();
    private int executionAttempts = 0;

    public TestOperator(ObjectMapper objectMapper, List<Remediator> remediators) {
      super(objectMapper, remediators);
    }

    @Override
    public ToolSpec spec() {
      return mockToolSpec;
    }

    @Override
    protected OperatorResult doExecute(RunContext ctx, Map<String, Object> inputs)
        throws Exception {
      executionAttempts++;

      if (alwaysFailWith != null) {
        throw alwaysFailWith;
      }

      if (failureOnAttempt.containsKey(executionAttempts)) {
        throw failureOnAttempt.get(executionAttempts);
      }

      if (successResultForAttempt.containsKey(executionAttempts)) {
        return successResultForAttempt.get(executionAttempts);
      }

      return OperatorResult.success();
    }

    @Override
    protected String getVersion() {
      return "1.0.0";
    }

    @Override
    protected String remediationHints() {
      return "timeout,backoff";
    }

    public void setFailureOnAttempt(int attempt, Exception exception) {
      this.failureOnAttempt.put(attempt, exception);
    }

    public void setSuccessResultForAttempt(int attempt, OperatorResult result) {
      this.successResultForAttempt.put(attempt, result);
    }

  }
}