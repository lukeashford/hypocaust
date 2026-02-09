package com.example.hypocaust.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.domain.OperatorLedger;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.Todo;
import com.example.hypocaust.domain.TodoStatus;
import com.example.hypocaust.logging.ModelCallLogger;
import com.example.hypocaust.operator.registry.OperatorRegistry;
import com.example.hypocaust.operator.result.OperatorResult;
import com.example.hypocaust.service.ArtifactNameGeneratorService;
import com.example.hypocaust.service.VersionManagementService;
import com.example.hypocaust.service.events.EventService;
import com.example.hypocaust.tool.InvokeTool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TodoHierarchyIntegrationTest {

  private InvokeTool invokeTool;
  private OperatorRegistry operatorRegistry;
  private EventService eventService;

  @BeforeEach
  void setUp() {
    operatorRegistry = mock(OperatorRegistry.class);
    eventService = mock(EventService.class);
    ModelCallLogger modelCallLogger = mock(ModelCallLogger.class);
    invokeTool = new InvokeTool(operatorRegistry, modelCallLogger);

    TaskExecutionContext ctx = new TaskExecutionContext(
        UUID.randomUUID(), UUID.randomUUID(), null,
        eventService, mock(VersionManagementService.class), mock(ArtifactNameGeneratorService.class)
    );
    TaskExecutionContextHolder.setContext(ctx);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void shouldMaintainHierarchyWithNestedInvocations() {
    // 1. Setup a parent todo in the list
    UUID rootTodoId = UUID.randomUUID();
    Todo rootTodo = new Todo(rootTodoId, "Root Task", TodoStatus.IN_PROGRESS, List.of());
    TaskExecutionContextHolder.getTodos().registerSubtodos(null, List.of(rootTodo));

    // Simulate that rootTodo is the "current" todo context
    TaskExecutionContextHolder.pushTodoId(rootTodoId);

    try {
      // 2. Mock a child operator
      Operator childOp = mock(Operator.class);
      OperatorSpec childSpec = new OperatorSpec("ChildOp", "1.0", "desc", List.of(), List.of());
      when(childOp.spec()).thenReturn(childSpec);

      when(childOp.execute(any(), any())).thenAnswer(invocation -> {
        UUID assignedTodoId = invocation.getArgument(1);
        assertNotNull(assignedTodoId);
        return OperatorResult.success("Child OK", Map.of(), Map.of("res", "val"));
      });

      when(operatorRegistry.get("ChildOp")).thenReturn(Optional.of(childOp));

      // 3. Create a ledger with TWO children
      OperatorLedger ledger = new OperatorLedger(
          new HashMap<>(Map.of("task", "test", "res", "val")),
          List.of(
              new OperatorLedger.ChildConfig("ChildOp", "Sub Task 1", Map.of(),
                  Map.of("res", "out1")),
              new OperatorLedger.ChildConfig("ChildOp", "Sub Task 2", Map.of(),
                  Map.of("res", "out2"))
          ),
          "task"
      );

      // 4. Invoke the ledger
      invokeTool.invoke(ledger);

      // 5. Verify the hierarchy
      List<Todo> topLevel = TaskExecutionContextHolder.getTodos().getList()
          .getTopLevel();
      assertEquals(1, topLevel.size());
      Todo updatedRoot = topLevel.get(0);
      assertEquals(rootTodoId, updatedRoot.id());
      assertEquals(2, updatedRoot.children().size());
      assertEquals("Sub Task 1", updatedRoot.children().get(0).description());
      assertEquals("Sub Task 2", updatedRoot.children().get(1).description());

    } finally {
      TaskExecutionContextHolder.popTodoId();
    }
  }

  @Test
  void shouldSkipStepForSingleChildLedger() {
    UUID rootTodoId = UUID.randomUUID();
    TaskExecutionContextHolder.pushTodoId(rootTodoId);

    try {
      Operator childOp = mock(Operator.class);
      when(childOp.spec()).thenReturn(
          new OperatorSpec("ChildOp", "1.0", "desc", List.of(), List.of()));

      // Child should receive the EXACT SAME rootTodoId
      when(childOp.execute(any(), any())).thenAnswer(invocation -> {
        UUID assignedTodoId = invocation.getArgument(1);
        assertEquals(rootTodoId, assignedTodoId);
        return OperatorResult.success("OK", Map.of(), Map.of("res", "val"));
      });

      when(operatorRegistry.get("ChildOp")).thenReturn(Optional.of(childOp));

      OperatorLedger ledger = new OperatorLedger(
          new HashMap<>(Map.of("task", "test")),
          List.of(new OperatorLedger.ChildConfig("ChildOp", "Single Task", Map.of(),
              Map.of("res", "out"))),
          "task"
      );

      invokeTool.invoke(ledger);

      // Verify no new todos were created in the context (since it's a skip step)
      assertEquals(0,
          TaskExecutionContextHolder.getTodos().getList().getTopLevel().size());

    } finally {
      TaskExecutionContextHolder.popTodoId();
    }
  }
}
