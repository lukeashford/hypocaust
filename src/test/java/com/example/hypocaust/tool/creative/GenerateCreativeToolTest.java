package com.example.hypocaust.tool.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.integration.ReplicateClient;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.rag.PlatformEmbeddingRegistry;
import com.example.hypocaust.rag.PlatformEmbeddingRegistry.SearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenerateCreativeToolTest {

  private PlatformEmbeddingRegistry modelRag;
  private ModelRegistry modelRegistry;
  private ReplicateClient replicateClient;
  private ObjectMapper objectMapper;
  private GenerateCreativeTool tool;

  @BeforeEach
  void setUp() {
    modelRag = mock(PlatformEmbeddingRegistry.class);
    modelRegistry = mock(ModelRegistry.class);
    replicateClient = mock(ReplicateClient.class);
    objectMapper = new ObjectMapper();
    tool = new GenerateCreativeTool(modelRag, modelRegistry, replicateClient, objectMapper);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void generate_noModelFound_returnsError() {
    when(modelRag.search(anyString())).thenReturn(List.of());

    var result = tool.generate("Generate a sunset image", ArtifactKind.IMAGE);

    assertThat(result.error()).contains("No suitable model found");
    assertThat(result.artifactName()).isNull();
  }

  @Test
  void generate_successfulGeneration_returnsSuccess() {
    // Setup context
    var context = mock(TaskExecutionContext.class);
    var artifactsContext = mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifactsContext);
    when(artifactsContext.add(any())).thenReturn("sunset-001");
    TaskExecutionContextHolder.setContext(context);

    // Setup RAG
    var modelDoc = "Replicate: stability-ai/sdxl\nVersion: abc123\nSummary: SDXL model";
    when(modelRag.search(anyString()))
        .thenReturn(List.of(new SearchResult("SDXL", modelDoc)));

    // Mock Haiku call (will fail since no chat model, but fallback handles it)
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("No LLM"));

    // Mock Replicate
    when(replicateClient.predict(anyString(), anyString(), anyString(), any()))
        .thenReturn(new TextNode("https://replicate.delivery/result.png"));

    var result = tool.generate("Generate a sunset", ArtifactKind.IMAGE);

    assertThat(result.artifactName()).isEqualTo("sunset-001");
    assertThat(result.summary()).contains("Generated IMAGE");
    assertThat(result.error()).isNull();
  }

  @Test
  void generate_replicateFailure_rollsBackAndReturnsError() {
    var context = mock(TaskExecutionContext.class);
    var artifactsContext = mock(ArtifactsContext.class);
    when(context.getTaskExecutionId()).thenReturn(UUID.randomUUID());
    when(context.getArtifacts()).thenReturn(artifactsContext);
    when(artifactsContext.add(any())).thenReturn("failed-001");
    TaskExecutionContextHolder.setContext(context);

    var modelDoc = "Replicate: stability-ai/sdxl\nVersion: abc123\nSummary: SDXL";
    when(modelRag.search(anyString()))
        .thenReturn(List.of(new SearchResult("SDXL", modelDoc)));
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("No LLM"));
    when(replicateClient.predict(anyString(), anyString(), anyString(), any()))
        .thenThrow(new ReplicateClient.ReplicateException("API timeout"));

    var result = tool.generate("Generate something", ArtifactKind.IMAGE);

    assertThat(result.error()).contains("Generation failed").contains("API timeout");
    verify(artifactsContext).rollbackPending("failed-001");
  }

  @Test
  void generateTitleAndDescription_fallback_whenLlmUnavailable() {
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("No LLM"));

    var result = tool.generateTitleAndDescription(
        "Create a beautiful landscape painting of mountains at sunset", ArtifactKind.IMAGE);

    // Fallback: title truncated to 60 chars, description is full task
    assertThat(result.title()).hasSizeLessThanOrEqualTo(60);
    assertThat(result.description()).isEqualTo(
        "Create a beautiful landscape painting of mountains at sunset");
  }

  @Test
  void generateTitleAndDescription_shortTask_usesFullTaskAsTitle() {
    when(modelRegistry.get(any(AnthropicChatModelSpec.class)))
        .thenThrow(new RuntimeException("No LLM"));

    var result = tool.generateTitleAndDescription("Cat photo", ArtifactKind.IMAGE);

    assertThat(result.title()).isEqualTo("Cat photo");
    assertThat(result.description()).isEqualTo("Cat photo");
  }
}
