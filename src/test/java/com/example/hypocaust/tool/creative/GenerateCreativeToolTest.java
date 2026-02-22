package com.example.hypocaust.tool.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.integration.ExecutionPlan;
import com.example.hypocaust.integration.ExecutionRouter;
import com.example.hypocaust.integration.ModelExecutor;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.SearchResult;
import com.example.hypocaust.service.TaskComplexityService;
import com.example.hypocaust.service.WordingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GenerateCreativeToolTest {

  private ModelEmbeddingRegistry modelRag;
  private ExecutionRouter executionRouter;
  private ModelExecutor modelExecutor;
  private TaskComplexityService complexityService;
  private WordingService wordingService;
  private ObjectMapper objectMapper;
  private GenerateCreativeTool tool;

  private ArtifactsContext artifactsContext;

  @BeforeEach
  void setUp() {
    modelRag = mock(ModelEmbeddingRegistry.class);
    executionRouter = mock(ExecutionRouter.class);
    modelExecutor = mock(ModelExecutor.class);
    complexityService = mock(TaskComplexityService.class);
    wordingService = mock(WordingService.class);
    objectMapper = new ObjectMapper();
    tool = new GenerateCreativeTool(modelRag, executionRouter, complexityService,
        wordingService, objectMapper);

    TaskExecutionContext context = mock(TaskExecutionContext.class);
    when(context.getTaskExecutionId()).thenReturn(java.util.UUID.randomUUID());
    artifactsContext = mock(ArtifactsContext.class);
    when(context.getArtifacts()).thenReturn(artifactsContext);
    TaskExecutionContextHolder.setContext(context);

    when(executionRouter.resolve(anyString())).thenReturn(modelExecutor);
  }

  @AfterEach
  void tearDown() {
    TaskExecutionContextHolder.clear();
  }

  @Test
  void generate_success() {
    // GIVEN
    String task = "Make a cute cat";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String owner = "stability-ai";
    String modelId = "sdxl";
    String description = "A high-quality image model";
    String bestPractices = "Use clear prompts";
    String tier = "balanced";

    when(complexityService.evaluate(anyString(), any())).thenReturn(tier);
    when(modelRag.search(anyString())).thenReturn(List.of(
        new SearchResult("SDXL", owner, modelId, description, bestPractices, tier, "REPLICATE")));

    var planInput = objectMapper.createObjectNode().put("prompt", "a cute cat");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());

    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Cute Cat");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn(
        "A very cute cat illustration");

    when(artifactsContext.add(any())).thenReturn("cute-cat-1");

    JsonNode executorOutput = objectMapper.valueToTree("https://replicate.com/output.png");
    when(modelExecutor.execute(eq("stability-ai"), eq("sdxl"), any())).thenReturn(executorOutput);
    when(modelExecutor.extractOutputUrl(executorOutput)).thenReturn(
        "https://replicate.com/output.png");

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactName()).isEqualTo("cute-cat-1");
    assertThat(result.summary()).contains("Generated IMAGE using SDXL");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.name().equals("cute-cat-1") &&
            artifact.url().equals("https://replicate.com/output.png") &&
            artifact.status().equals(ArtifactStatus.CREATED)
    ));
  }

  @Test
  void generate_noModelFound_returnsError() {
    when(complexityService.evaluate(anyString(), any())).thenReturn("balanced");
    when(modelRag.search(anyString())).thenReturn(List.of());

    var result = tool.generate("Generate a sunset image", ArtifactKind.IMAGE);

    assertThat(result.error()).contains("No suitable model found");
    assertThat(result.artifactName()).isNull();
  }

  @Test
  void generate_planReturnsErrorMessage_returnsError() {
    // GIVEN
    String task = "Make a video";
    ArtifactKind kind = ArtifactKind.VIDEO;
    String owner = "lucataco";
    String modelId = "animate-diff";
    String tier = "balanced";

    when(complexityService.evaluate(anyString(), any())).thenReturn(tier);
    when(modelRag.search(anyString())).thenReturn(
        List.of(new SearchResult("AnimateDiff", owner, modelId, "A video model",
            "Keep it short", tier, "REPLICATE")));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(ExecutionPlan.error("Missing video length"));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isEqualTo("Missing video length");
  }

  @Test
  void generate_executorCallFails_rollsBackArtifact() {
    // GIVEN
    String task = "Make a cat";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String owner = "stability-ai";
    String modelId = "sdxl";
    String tier = "balanced";

    when(complexityService.evaluate(anyString(), any())).thenReturn(tier);
    when(modelRag.search(anyString())).thenReturn(List.of(
        new SearchResult("SDXL", owner, modelId, "desc", "best", tier, "REPLICATE")));

    var planInput = objectMapper.createObjectNode().put("prompt", "cat");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Cat");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A cat");
    when(artifactsContext.add(any())).thenReturn("cat-1");

    when(modelExecutor.execute(anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("Provider API timeout"));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("Provider API timeout");
    assertThat(result.artifactName()).isNull();
    verify(artifactsContext).rollbackPending("cat-1");
  }

  @Test
  void generate_nullOutputUrl_returnsError() {
    // GIVEN
    String task = "Make something";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String owner = "stability-ai";
    String modelId = "sdxl";
    String tier = "balanced";

    when(complexityService.evaluate(anyString(), any())).thenReturn(tier);
    when(modelRag.search(anyString())).thenReturn(List.of(
        new SearchResult("SDXL", owner, modelId, "desc", "best", tier, "REPLICATE")));

    var planInput = objectMapper.createObjectNode().put("prompt", "thing");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Thing");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A thing");
    when(artifactsContext.add(any())).thenReturn("thing-1");

    JsonNode nullOutput = objectMapper.nullNode();
    when(modelExecutor.execute(anyString(), anyString(), any())).thenReturn(nullOutput);
    when(modelExecutor.extractOutputUrl(nullOutput)).thenReturn("null");

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("no usable output URL");
    verify(artifactsContext).rollbackPending("thing-1");
  }

  @Nested
  class SubstituteArtifacts {

    @Test
    void replacesPlaceholderWithUrl() throws Exception {
      var input = objectMapper.readTree("{\"image\": \"@photo\"}");
      var artifact = Artifact.builder()
          .name("photo").kind(ArtifactKind.IMAGE).title("T").description("D")
          .status(ArtifactStatus.CREATED).url("https://cdn.example.com/photo.png").build();

      var result = tool.substituteArtifacts(input, List.of(artifact));
      assertThat(result.get("image").asText()).isEqualTo("https://cdn.example.com/photo.png");
    }

    @Test
    void nullUrl_leavesPlaceholderAsIs() throws Exception {
      var input = objectMapper.readTree("{\"image\": \"@photo\"}");
      var artifact = Artifact.builder()
          .name("photo").kind(ArtifactKind.IMAGE).title("T").description("D")
          .status(ArtifactStatus.GESTATING).build();

      var result = tool.substituteArtifacts(input, List.of(artifact));
      assertThat(result.get("image").asText()).isEqualTo("@photo");
    }
  }
}
