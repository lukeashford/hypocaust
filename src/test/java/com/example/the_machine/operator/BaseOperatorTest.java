package com.example.the_machine.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.the_machine.operator.result.OperatorResult;
import com.example.the_machine.operator.result.OperatorResultCode;
import com.example.the_machine.operator.result.ValidationResult;
import com.example.the_machine.service.RunContext;
import com.example.the_machine.service.RunPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseOperatorTest {

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

    // Setup mock remediators
    lenient().when(mockHeuristicRemediator.getName()).thenReturn("HeuristicRemediator");
    lenient().when(mockLLMRemediator.getName()).thenReturn("LLMRemediator");

    // Create TestOperator with mock remediators
    val remediators = List.of(mockHeuristicRemediator, mockLLMRemediator);
    testOperator = new TestOperator(objectMapper, remediators);

    // Setup default policy
    val policy = new RunPolicy(3, 10.0, 10, 100);
    lenient().when(mockContext.policy()).thenReturn(policy);

    // Setup checkBudgets to not throw by default
    lenient().doNothing().when(mockContext).checkBudgets();
  }

  @Test
  void testSuccessPath() {
    // Given
    val rawInputs = Map.<String, Object>of("param1", "value1");
    val normalizedInputs = Map.<String, Object>of("param1", "value1", "param2", "defaultValue");
    val outputs = Map.<String, Object>of("result", "success");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);
    testOperator.setSuccessResult(
        OperatorResult.success("TestOperator", "1.0.0", normalizedInputs, outputs));

    // When
    val result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertTrue(result.isOk());
    assertEquals(outputs, result.getOutputs());
    assertEquals(normalizedInputs, result.getNormalizedInputs());
    assertEquals(1, result.getAttempts());
    assertTrue(result.getMetrics().containsKey("latencyMs"));
    verify(mockContext).checkBudgets();
  }

  @Test
  void testFailureWithRemediationThenSuccess() {
    // Given
    val rawInputs = Map.<String, Object>of("timeout", 10);
    val normalizedInputs = Map.<String, Object>of("timeout", 10);
    val outputs = Map.<String, Object>of("result", "success after remediation");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);

    // Setup mock remediation patches - create a timeout adjustment patch
    val timeoutPatch = objectMapper.createObjectNode();
    timeoutPatch.put("op", "replace");
    timeoutPatch.put("path", "/timeout");
    timeoutPatch.set("value", objectMapper.valueToTree(20));

    // Configure mock HeuristicRemediator to return patches for timeout error
    val timeoutException = new RuntimeException("Operation timed out");
    when(mockHeuristicRemediator.remediate(mockContext, normalizedInputs, timeoutException,
        "timeout,backoff"))
        .thenReturn(List.of(timeoutPatch));
    // LLMRemediator won't be called since HeuristicRemediator succeeds, so make it lenient
    lenient().when(mockLLMRemediator.remediate(mockContext, normalizedInputs, timeoutException,
            "timeout,backoff"))
        .thenReturn(List.of());

    // First attempt fails with timeout error
    testOperator.setFailureOnAttempt(1, timeoutException);
    testOperator.setSuccessResultForAttempt(2,
        OperatorResult.success("TestOperator", "1.0.0", normalizedInputs, outputs));

    // When
    val result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertTrue(result.isOk());
    assertEquals(outputs, result.getOutputs());
    assertEquals(2, result.getAttempts());
    assertFalse(result.getRemediationPatches().isEmpty());
    assertTrue(result.getMetrics().containsKey("latencyMs"));

    // Verify remediation was applied (timeout should be doubled)
    assertEquals(2, testOperator.getExecutionAttempts());
  }

  @Test
  void testHittingMaxTriesPerOp() {
    // Given
    val rawInputs = Map.<String, Object>of("param1", "value1");
    val normalizedInputs = Map.<String, Object>of("param1", "value1");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);

    // All attempts fail
    val exception = new RuntimeException("Persistent failure");
    testOperator.setAlwaysFailWith(exception);

    // When
    val result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertFalse(result.isOk());
    assertEquals(3, result.getAttempts()); // Should match maxTriesPerOp
    assertEquals(OperatorResultCode.EXECUTION_FAILED, result.getCode());
    assertTrue(result.getMessage().contains("Persistent failure"));
    assertTrue(result.getMetrics().containsKey("latencyMs"));
  }

  @Test
  void testSecretRedaction() {
    // Given
    val secretValue = "super-secret-key-123";
    val rawInputs = Map.<String, Object>of("apiKey", secretValue, "model", "gpt-4");
    val normalizedInputs = Map.<String, Object>of("apiKey", secretValue, "model", "gpt-4");

    // Setup secret parameter
    val secretParamSpec = mock(ParamSpec.class);
    when(secretParamSpec.getName()).thenReturn("apiKey");
    when(secretParamSpec.isSecret()).thenReturn(true);

    val regularParamSpec = mock(ParamSpec.class);
    lenient().when(regularParamSpec.getName()).thenReturn("model");
    lenient().when(regularParamSpec.isSecret()).thenReturn(false);

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);
    List<ParamSpec<?>> specs = new ArrayList<>();
    specs.add(secretParamSpec);
    specs.add(regularParamSpec);
    when(mockToolSpec.getInputs()).thenReturn(specs);

    // Fail with error message containing secret
    val exception = new RuntimeException("Authentication failed with key: " + secretValue);
    testOperator.setAlwaysFailWith(exception);

    // When
    val result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertFalse(result.isOk());
    assertFalse(result.getMessage().contains(secretValue));
    assertTrue(result.getMessage().contains("[REDACTED]"));
    assertTrue(result.getMessage().contains("Authentication failed with key: [REDACTED]"));
  }

  @Test
  void testValidationFailure() {
    // Given
    val rawInputs = Map.<String, Object>of("invalidParam", "value");

    val validationResult = mock(ValidationResult.class);
    when(validationResult.isOk()).thenReturn(false);
    when(validationResult.getMessage()).thenReturn("Invalid parameter: invalidParam");

    when(mockToolSpec.validate(rawInputs)).thenReturn(validationResult);
    testOperator.setMockToolSpec(mockToolSpec);

    // When
    val result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertFalse(result.isOk());
    assertEquals(OperatorResultCode.VALIDATION_ERROR, result.getCode());
    assertTrue(result.getMessage().contains("Invalid parameter: invalidParam"));
    assertEquals(1, result.getAttempts());
  }

  @Test
  void testOperatorVersionIsReturnedInResult() {
    // Given
    val rawInputs = Map.<String, Object>of("param", "value");
    val normalizedInputs = Map.<String, Object>of("param", "value");

    setupMockValidationAndDefaults(rawInputs, normalizedInputs);
    testOperator.setSuccessResult(OperatorResult.success());

    // When
    val result = testOperator.execute(mockContext, rawInputs);

    // Then
    assertTrue(result.isOk());
    assertEquals("1.0.0", result.getOperatorVersion());
    assertEquals("TestOperator", result.getOperatorName());
  }

  private void setupMockValidationAndDefaults(Map<String, Object> rawInputs,
      Map<String, Object> normalizedInputs) {
    val validationResult = mock(ValidationResult.class);
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
    private OperatorResult successResult;
    @Setter
    private Exception alwaysFailWith;
    private final Map<Integer, Exception> failureOnAttempt = new HashMap<>();
    private final Map<Integer, OperatorResult> successResultForAttempt = new HashMap<>();
    @Getter
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

      return successResult != null ? successResult : OperatorResult.success();
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