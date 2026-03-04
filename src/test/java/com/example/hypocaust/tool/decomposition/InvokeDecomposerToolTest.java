package com.example.hypocaust.tool.decomposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.agent.TodoExecutor;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TodosContext;
import com.example.hypocaust.service.events.EventService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvokeDecomposerToolTest {

  private Decomposer decomposer;
  private InvokeDecomposerTool invokeDecomposerTool;

  @BeforeEach
  void setUp() {
    decomposer = mock(Decomposer.class);
    TodoExecutor todoExecutor = new TodoExecutor();
    invokeDecomposerTool = new InvokeDecomposerTool(decomposer, todoExecutor);

    var context = mock(TaskExecutionContext.class);
    var eventService = mock(EventService.class);
    var todosContext = new TodosContext(UUID.randomUUID(), eventService);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getTodos()).thenReturn(todosContext);
    TaskExecutionContextHolder.setContext(context);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void invoke_successfulDecomposition_returnsSuccess() {
    var expectedResult = DecomposerResult.success("Image generated", List.of("img-001"));
    when(decomposer.execute(anyString(), any())).thenReturn(expectedResult);

    var result = invokeDecomposerTool.invoke("Generate an image", "Generating image", null);

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Image generated");
    assertThat(result.artifactNames()).containsExactly("img-001");
  }

  @Test
  void invoke_withContextBrief_passesToDecomposer() {
    var expectedResult = DecomposerResult.success("Portrait generated", List.of("portrait-1"));
    var brief = List.of("Biscuit is an orange tabby cat", "Style: watercolor");
    when(decomposer.execute(anyString(), any())).thenReturn(expectedResult);

    var result = invokeDecomposerTool.invoke("Generate a portrait of Biscuit",
        "Generating portrait", brief);

    assertThat(result.success()).isTrue();
    verify(decomposer).execute("Generate a portrait of Biscuit", brief);
  }

  @Test
  void invoke_failedDecomposition_returnsFailure() {
    var expectedResult = DecomposerResult.failure("No suitable model");
    when(decomposer.execute(anyString(), any())).thenReturn(expectedResult);

    var result = invokeDecomposerTool.invoke("Generate something", "Trying generation", null);

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("No suitable model");
  }

  @Test
  void invoke_decomposerThrows_returnsFailure() {
    when(decomposer.execute(anyString(), any())).thenThrow(new RuntimeException("LLM unavailable"));

    var result = invokeDecomposerTool.invoke("Do something", "Attempting task", null);

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("LLM unavailable");
  }

  @Test
  void invoke_incrementsAndDecrementsDepth() {
    var initialDepth = TaskExecutionContextHolder.getDepth();
    when(decomposer.execute(anyString(), any())).thenAnswer(inv -> {
      assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth + 1);
      return DecomposerResult.success("done", List.of());
    });

    invokeDecomposerTool.invoke("task", "label", null);

    assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth);
  }

  @Test
  void invoke_decrementsDepthOnException() {
    var initialDepth = TaskExecutionContextHolder.getDepth();
    when(decomposer.execute(anyString(), any())).thenThrow(new RuntimeException("boom"));

    invokeDecomposerTool.invoke("task", "label", null);

    assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth);
  }
}
