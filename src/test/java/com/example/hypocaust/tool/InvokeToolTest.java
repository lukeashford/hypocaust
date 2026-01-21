package com.example.hypocaust.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.OperatorLedger;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.Operator;
import com.example.hypocaust.operator.OperatorSpec;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvokeToolTest {

  @Mock
  private OperatorRegistry operatorRegistry;

  @Mock
  private ModelCallLogger modelCallLogger;

  @Mock
  private Operator mockOperator;

  private InvokeTool invokeTool;

  @BeforeEach
  void setUp() {
    invokeTool = new InvokeTool(operatorRegistry, modelCallLogger);
  }

  @Test
  void shouldFailWhenOperatorNotFound() {
    // Given
    String operatorName = "NonExistentOperator";
    OperatorLedger ledger = new OperatorLedger(
        new HashMap<>(),
        List.of(new OperatorLedger.ChildConfig(operatorName, Map.of(), Map.of())),
        "result"
    );

    when(operatorRegistry.get(operatorName)).thenReturn(Optional.empty());

    // When
    OperatorResult result = invokeTool.invoke(ledger);

    // Then
    assertFalse(result.ok());
    assertTrue(result.message().contains("No operator found with name: " + operatorName));
  }

  @Test
  void shouldFailWhenFinalOutputKeyIsMissing() {
    // Given
    String operatorName = "MockOperator";
    OperatorSpec spec = new OperatorSpec(operatorName, "1.0", "desc", List.of(), List.of());
    when(mockOperator.spec()).thenReturn(spec);
    when(mockOperator.execute(any())).thenReturn(OperatorResult.success("ok", Map.of(), Map.of()));
    when(operatorRegistry.get(operatorName)).thenReturn(Optional.of(mockOperator));

    OperatorLedger ledger = new OperatorLedger(
        new HashMap<>(),
        List.of(new OperatorLedger.ChildConfig(operatorName, Map.of(), Map.of())),
        "missingKey"
    );

    // When
    OperatorResult result = invokeTool.invoke(ledger);

    // Then
    assertFalse(result.ok());
    assertTrue(
        result.message().contains("Final output key not found in ledger values: missingKey"));
  }

  @Test
  void shouldSucceedWhenEverythingIsCorrect() {
    // Given
    String operatorName = "MockOperator";
    OperatorSpec spec = new OperatorSpec(operatorName, "1.0", "desc", List.of(), List.of());
    when(mockOperator.spec()).thenReturn(spec);
    when(mockOperator.execute(any())).thenReturn(OperatorResult.success("ok", Map.of(), Map.of()));
    when(operatorRegistry.get(operatorName)).thenReturn(Optional.of(mockOperator));

    Map<String, Object> values = new HashMap<>();
    values.put("someKey", "someValue");

    OperatorLedger ledger = new OperatorLedger(
        values,
        List.of(new OperatorLedger.ChildConfig(operatorName, Map.of(), Map.of())),
        "someKey"
    );

    // When
    OperatorResult result = invokeTool.invoke(ledger);

    // Then
    assertTrue(result.ok());
    assertEquals("someValue", result.outputs().get("result"));
  }
}
