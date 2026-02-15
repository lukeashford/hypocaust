package com.example.hypocaust.tool.decomposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.Decomposer;
import com.example.hypocaust.agent.DecomposerResult;
import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.domain.TodosContext;
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
    invokeDecomposerTool = new InvokeDecomposerTool(decomposer);

    var context = mock(TaskExecutionContext.class);
    var todosContext = mock(TodosContext.class);
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
    when(decomposer.execute(anyString())).thenReturn(expectedResult);

    var result = invokeDecomposerTool.invoke("Generate an image", "Generating image");

    assertThat(result.success()).isTrue();
    assertThat(result.summary()).isEqualTo("Image generated");
    assertThat(result.artifactNames()).containsExactly("img-001");
  }

  @Test
  void invoke_failedDecomposition_returnsFailure() {
    var expectedResult = DecomposerResult.failure("No suitable model");
    when(decomposer.execute(anyString())).thenReturn(expectedResult);

    var result = invokeDecomposerTool.invoke("Generate something", "Trying generation");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("No suitable model");
  }

  @Test
  void invoke_decomposerThrows_returnsFailure() {
    when(decomposer.execute(anyString())).thenThrow(new RuntimeException("LLM unavailable"));

    var result = invokeDecomposerTool.invoke("Do something", "Attempting task");

    assertThat(result.success()).isFalse();
    assertThat(result.errorMessage()).isEqualTo("LLM unavailable");
  }

  @Test
  void invoke_incrementsAndDecrementsDepth() {
    var initialDepth = TaskExecutionContextHolder.getDepth();
    when(decomposer.execute(anyString())).thenAnswer(inv -> {
      assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth + 1);
      return DecomposerResult.success("done", List.of());
    });

    invokeDecomposerTool.invoke("task", "label");

    assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth);
  }

  @Test
  void invoke_decrementsDepthOnException() {
    var initialDepth = TaskExecutionContextHolder.getDepth();
    when(decomposer.execute(anyString())).thenThrow(new RuntimeException("boom"));

    invokeDecomposerTool.invoke("task", "label");

    assertThat(TaskExecutionContextHolder.getDepth()).isEqualTo(initialDepth);
  }
}
