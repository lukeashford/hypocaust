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

class DeleteArtifactToolTest {

  private DeleteArtifactTool tool;

  @BeforeEach
  void setUp() {
    tool = new DeleteArtifactTool();
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void delete_nullName_returnsError() {
    var result = tool.delete(null);

    assertThat(result.error()).isEqualTo("Artifact name is required");
    assertThat(result.artifactName()).isNull();
  }

  @Test
  void delete_blankName_returnsError() {
    var result = tool.delete("   ");

    assertThat(result.error()).isEqualTo("Artifact name is required");
  }

  @Test
  void delete_successfulDeletion_returnsSuccess() {
    var context = mock(TaskExecutionContext.class);
    var artifactsContext = mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifactsContext);
    TaskExecutionContextHolder.setContext(context);

    var result = tool.delete("landscape-001");

    assertThat(result.artifactName()).isEqualTo("landscape-001");
    assertThat(result.summary()).contains("marked for deletion");
    assertThat(result.error()).isNull();
  }

  @Test
  void delete_contextNotSet_returnsError() {
    // No context set, getContext() will throw
    var result = tool.delete("anything");

    assertThat(result.error()).isNotNull();
  }
}
