package com.example.hypocaust.tool.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RestoreArtifactToolTest {

  private RestoreArtifactTool tool;

  @BeforeEach
  void setUp() {
    tool = new RestoreArtifactTool();
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void restore_nullArtifactName_returnsError() {
    var result = tool.restore(null, "initial_character_designs");

    assertThat(result.error()).isEqualTo("Artifact name is required");
    assertThat(result.restoredName()).isNull();
  }

  @Test
  void restore_blankArtifactName_returnsError() {
    var result = tool.restore("   ", "initial_character_designs");

    assertThat(result.error()).isEqualTo("Artifact name is required");
  }

  @Test
  void restore_nullExecutionName_returnsError() {
    var result = tool.restore("protagonist", null);

    assertThat(result.error()).isEqualTo("Execution name is required");
    assertThat(result.restoredName()).isNull();
  }

  @Test
  void restore_blankExecutionName_returnsError() {
    var result = tool.restore("protagonist", "  ");

    assertThat(result.error()).isEqualTo("Execution name is required");
  }

  @Test
  void restore_contextNotSet_returnsError() {
    // No context set — getContext() will throw
    var result = tool.restore("protagonist", "initial_character_designs");

    assertThat(result.error()).isNotNull();
  }

  @Test
  void restore_nameAvailable_returnsSameName() {
    var context = mock(TaskExecutionContext.class);
    var artifactsContext = mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifactsContext);
    when(artifactsContext.restore("protagonist", "initial_character_designs"))
        .thenReturn("protagonist");
    TaskExecutionContextHolder.setContext(context);

    var result = tool.restore("protagonist", "initial_character_designs");

    assertThat(result.error()).isNull();
    assertThat(result.originalName()).isEqualTo("protagonist");
    assertThat(result.restoredName()).isEqualTo("protagonist");
    assertThat(result.executionName()).isEqualTo("initial_character_designs");
    assertThat(result.summary()).contains("protagonist").contains("initial_character_designs");
  }

  @Test
  void restore_nameTaken_returnsAlternativeName() {
    var context = mock(TaskExecutionContext.class);
    var artifactsContext = mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifactsContext);
    when(artifactsContext.restore("protagonist", "initial_character_designs"))
        .thenReturn("protagonist_2");
    TaskExecutionContextHolder.setContext(context);

    var result = tool.restore("protagonist", "initial_character_designs");

    assertThat(result.error()).isNull();
    assertThat(result.originalName()).isEqualTo("protagonist");
    assertThat(result.restoredName()).isEqualTo("protagonist_2");
    assertThat(result.summary()).contains("protagonist_2").contains("protagonist").contains("taken");
  }

  @Test
  void restore_trailingWhitespace_isTrimmed() {
    var context = mock(TaskExecutionContext.class);
    var artifactsContext = mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifactsContext);
    when(artifactsContext.restore("protagonist", "initial_character_designs"))
        .thenReturn("protagonist");
    TaskExecutionContextHolder.setContext(context);

    var result = tool.restore("  protagonist  ", "  initial_character_designs  ");

    assertThat(result.error()).isNull();
    assertThat(result.originalName()).isEqualTo("protagonist");
    assertThat(result.executionName()).isEqualTo("initial_character_designs");
  }
}
