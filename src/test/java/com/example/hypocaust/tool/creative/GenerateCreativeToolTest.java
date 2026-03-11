package com.example.hypocaust.tool.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactAction;
import com.example.hypocaust.domain.ArtifactIntent;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.OutputSpec;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.models.ExecutionResult;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.models.ModelExecutor;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.ModelSearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class GenerateCreativeToolTest {

  private ModelEmbeddingRegistry modelRag;
  private ModelExecutor modelExecutor;
  private WordingService wordingService;
  private ObjectMapper objectMapper;
  private GenerateCreativeTool tool;

  private ArtifactsContext artifactsContext;

  @BeforeEach
  void setUp() {
    modelRag = mock(ModelEmbeddingRegistry.class);
    ExecutionRouter executionRouter = mock(ExecutionRouter.class);
    modelExecutor = mock(ModelExecutor.class);
    wordingService = mock(WordingService.class);
    objectMapper = new ObjectMapper();
    tool = new GenerateCreativeTool(modelRag, executionRouter,
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

  private ArtifactIntent addIntent(ArtifactKind kind, String description) {
    return ArtifactIntent.builder()
        .action(ArtifactAction.ADD)
        .kind(kind)
        .description(description)
        .preferredName("test_artifact")
        .preferredTitle("Test Artifact")
        .build();
  }

  @Test
  void generate_success() {
    // GIVEN
    String task = "Make a cute cat";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());
    var outputSpec = new OutputSpec(kind, "the image");
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new ModelSearchResult("SDXL", "stability-ai", "sdxl", "A high-quality image model",
            "Use clear prompts", "balanced", "REPLICATE",
            Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    Artifact mockGestating = Artifact.builder()
        .name("cute-cat-1").kind(kind).title("Cute Cat")
        .description("A very cute cat illustration")
        .status(ArtifactStatus.GESTATING)
        .build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any())).thenReturn(
        mockGestating);

    var providerInput = objectMapper.createObjectNode().put("prompt", "a cute cat");
    var finalizedArtifact = Artifact.builder()
        .name("cute-cat-1").kind(kind).title("Cute Cat")
        .description("A very cute cat illustration")
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/hash.png")
        .mimeType("image/png")
        .build();

    when(modelExecutor.run(anyList(), anyString(), any(ModelSearchResult.class),
        anyList()))
        .thenReturn(new ExecutionResult(List.of(finalizedArtifact), providerInput));

    // WHEN
    var result = tool.generate(task, List.of(addIntent(kind, "A cute cat image")));

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactNames()).containsExactly("cute-cat-1");
    assertThat(result.summary()).contains("Generated artifacts: cute-cat-1 using SDXL");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.name().equals("cute-cat-1") &&
            artifact.storageKey().equals("blobs/ab/cd/hash.png") &&
            artifact.status().equals(ArtifactStatus.MANIFESTED)
    ));
  }

  @Test
  void generate_noModelFound_returnsError() {
    String task = "Generate a sunset image";

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of());

    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(ArtifactKind.IMAGE), any()))
        .thenReturn(Artifact.builder()
            .name("sunset-1").kind(ArtifactKind.IMAGE).title("Sunset").description("A sunset")
            .status(ArtifactStatus.GESTATING).build());

    var result = tool.generate(task, List.of(addIntent(ArtifactKind.IMAGE, "A sunset image")));

    assertThat(result.error()).contains("No suitable model found");
    assertThat(result.artifactNames()).isNull();
  }

  @Test
  void generate_planReturnsErrorMessage_fallsThrough() {
    // GIVEN — only one model candidate, executor fails → all models exhausted
    String task = "Make a video";
    ArtifactKind kind = ArtifactKind.VIDEO;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());
    var outputSpec = new OutputSpec(kind, "the video");
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(
        List.of(new ModelSearchResult("AnimateDiff", "lucataco", "animate-diff", "A video model",
            "Keep it short", "balanced", "REPLICATE", Set.of(ArtifactKind.TEXT), null,
            Set.of(outputSpec))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    Artifact mockGestating = Artifact.builder()
        .name("video-1").kind(kind).title("Video").description("A video")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any())).thenReturn(
        mockGestating);

    when(modelExecutor.run(anyList(), anyString(), any(ModelSearchResult.class),
        anyList()))
        .thenThrow(new RuntimeException("Planning failed: Missing video length"));

    // WHEN
    var result = tool.generate(task, List.of(addIntent(kind, "A video")));

    // THEN
    assertThat(result.error()).contains("All models failed");
    assertThat(result.error()).contains("Missing video length");
  }

  @Test
  void generate_executorCallFails_rollsBackArtifact() {
    // GIVEN
    String task = "Make a cat";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());
    var outputSpec = new OutputSpec(kind, "the image");
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new ModelSearchResult("SDXL", "stability-ai", "sdxl", "desc", "best", "balanced",
            "REPLICATE",
            Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    Artifact mockGestating = Artifact.builder()
        .name("cat-1").kind(kind).title("Cat").description("A cat")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any())).thenReturn(
        mockGestating);

    when(modelExecutor.run(anyList(), anyString(), any(ModelSearchResult.class),
        anyList()))
        .thenThrow(new RuntimeException("Provider API timeout"));

    // WHEN
    var result = tool.generate(task, List.of(addIntent(kind, "A cat image")));

    // THEN — doExecute throws → orchestrate rolls back gestating artifacts
    assertThat(result.error()).contains("All models failed");
    verify(artifactsContext).rollbackPending("cat-1");
  }

  @Test
  void generate_nullOutputUrl_rollsBackAndFails() {
    // GIVEN — executor throws when output is "null"
    String task = "Make something";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());
    var outputSpec = new OutputSpec(kind, "the thing");
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new ModelSearchResult("SDXL", "stability-ai", "sdxl", "desc", "best", "balanced",
            "REPLICATE",
            Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    Artifact mockGestating = Artifact.builder()
        .name("thing-1").kind(kind).title("Thing").description("A thing")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any())).thenReturn(
        mockGestating);

    when(modelExecutor.run(anyList(), anyString(), any(ModelSearchResult.class),
        anyList()))
        .thenThrow(new IllegalStateException("Model returned no usable output"));

    // WHEN
    var result = tool.generate(task, List.of(addIntent(kind, "Something")));

    // THEN
    assertThat(result.error()).contains("All models failed");
    verify(artifactsContext).rollbackPending("thing-1");
  }

  @Test
  void generate_text_inlineContent() {
    // GIVEN
    String task = "Write a poem";
    ArtifactKind kind = ArtifactKind.TEXT;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("high")
            .searchString(task).build());
    var outputSpec = new OutputSpec(kind, "the poem");
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new ModelSearchResult("Claude Opus", "anthropic", "claude-3-opus", "desc", "best", "high",
            "OPENROUTER", Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    Artifact mockGestating = Artifact.builder()
        .name("poem-1").kind(kind).title("Poem").description("A poem")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any())).thenReturn(
        mockGestating);

    String poemText = "Roses are red...";
    var providerInput = objectMapper.createObjectNode().put("prompt", "poem");
    var finalizedArtifact = Artifact.builder()
        .name("poem-1").kind(kind).title("Poem").description("A poem")
        .status(ArtifactStatus.MANIFESTED)
        .inlineContent(new TextNode(poemText))
        .mimeType("text/plain")
        .build();

    when(modelExecutor.run(anyList(), eq(task), any(ModelSearchResult.class),
        anyList()))
        .thenReturn(new ExecutionResult(List.of(finalizedArtifact), providerInput));

    // WHEN
    var result = tool.generate(task, List.of(addIntent(kind, "A poem")));

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactNames()).containsExactly("poem-1");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.kind() == ArtifactKind.TEXT &&
            artifact.storageKey() == null &&
            artifact.inlineContent().asText().equals(poemText) &&
            artifact.status() == ArtifactStatus.MANIFESTED
    ));
  }

  @Test
  void generate_fallbackToSecondModel_onFirstModelFailure() {
    // ... existing test ...
  }

  @Test
  void generate_skipsSamePlatform_onPlatformFailure() {
    // GIVEN — three models: 1 & 2 on same platform, 3 on different platform.
    // Model 1 fails with 405 (Platform Failure).
    // Model 2 should be skipped.
    // Model 3 should be tried.
    String task = "Skip platform test";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());

    var outputSpec = new OutputSpec(kind, "output");
    var model1 = new ModelSearchResult("M1", "O1", "ID1", "D1", "B1", "balanced", "PLATFORM_A",
        Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec));
    var model2 = new ModelSearchResult("M2", "O1", "ID2", "D2", "B2", "balanced", "PLATFORM_A",
        Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec));
    var model3 = new ModelSearchResult("M3", "O2", "ID3", "D3", "B3", "balanced", "PLATFORM_B",
        Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec));

    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(model1, model2, model3));

    Artifact mockGestating = Artifact.builder()
        .name("test-1").kind(kind).title("Test Title").description("Test Description")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any()))
        .thenReturn(mockGestating);

    // Model 1: fails with 405 Method Not Allowed
    when(modelExecutor.run(anyList(), anyString(), eq(model1), anyList()))
        .thenThrow(HttpClientErrorException.create(HttpStatus.METHOD_NOT_ALLOWED, "405", null, null,
            StandardCharsets.UTF_8));

    // Model 3: succeeds
    var finalizedArtifact = Artifact.builder()
        .name("test-1").kind(kind).title("Test Title").description("Test Description")
        .status(ArtifactStatus.MANIFESTED).build();
    when(modelExecutor.run(anyList(), anyString(), eq(model3), anyList()))
        .thenReturn(
            new ExecutionResult(List.of(finalizedArtifact), objectMapper.createObjectNode()));

    // WHEN
    tool.generate(task, List.of(addIntent(kind, "test")));

    // THEN
    verify(modelExecutor).run(anyList(), anyString(), eq(model1), anyList());
    verify(modelExecutor, never()).run(anyList(), anyString(), eq(model2), anyList());
    verify(modelExecutor).run(anyList(), anyString(), eq(model3), anyList());
  }

  @Test
  void generate_retriesSamePlatform_onNonPlatformFailure() {
    // GIVEN — two models on same platform.
    // Model 1 fails with 500 (Internal Server Error - NOT a Platform Failure by our logic).
    // Model 2 should be tried even though it's the same platform.
    String task = "Retry platform test";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());

    var outputSpec = new OutputSpec(kind, "output");
    var model1 = new ModelSearchResult("M1", "O1", "ID1", "D1", "B1", "balanced", "PLATFORM_A",
        Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec));
    var model2 = new ModelSearchResult("M2", "O1", "ID2", "D2", "B2", "balanced", "PLATFORM_A",
        Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec));

    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(model1, model2));

    Artifact mockGestating = Artifact.builder()
        .name("test-1").kind(kind).title("Test Title").description("Test Description")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any()))
        .thenReturn(mockGestating);

    // Model 1: fails with 500 Internal Server Error
    when(modelExecutor.run(anyList(), anyString(), eq(model1), anyList()))
        .thenThrow(
            HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "500", null, null,
                StandardCharsets.UTF_8));

    // Model 2: succeeds
    var finalizedArtifact = Artifact.builder()
        .name("test-1").kind(kind).title("Test Title").description("Test Description")
        .status(ArtifactStatus.MANIFESTED).build();
    when(modelExecutor.run(anyList(), anyString(), eq(model2), anyList()))
        .thenReturn(
            new ExecutionResult(List.of(finalizedArtifact), objectMapper.createObjectNode()));

    // WHEN
    tool.generate(task, List.of(addIntent(kind, "test")));

    // THEN
    verify(modelExecutor).run(anyList(), anyString(), eq(model1), anyList());
    verify(modelExecutor).run(anyList(), anyString(), eq(model2), anyList());
  }

  @Test
  void generate_respectsMaxAttempts() {
    // GIVEN — 10 models, all fail.
    String task = "Max attempts test";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(ModelRequirement.builder().inputs(Set.of()).outputs(Set.of()).tier("balanced")
            .searchString(task).build());

    var outputSpec = new OutputSpec(kind, "output");
    java.util.ArrayList<ModelSearchResult> models = new java.util.ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      models.add(new ModelSearchResult("M" + i, "O", "ID" + i, "D", "B", "balanced", "P" + i,
          Set.of(ArtifactKind.TEXT), null, Set.of(outputSpec)));
    }

    when(modelRag.search(any(ModelRequirement.class))).thenReturn(models);

    Artifact mockGestating = Artifact.builder()
        .name("test-1").kind(kind).title("Test Title").description("Test Description")
        .status(ArtifactStatus.GESTATING).build();
    when(artifactsContext.add(anyString(), anyString(), anyString(), eq(kind), any()))
        .thenReturn(mockGestating);

    when(modelExecutor.run(anyList(), anyString(), any(ModelSearchResult.class), anyList()))
        .thenThrow(new RuntimeException("Fail"));

    // WHEN
    var result = tool.generate(task, List.of(addIntent(kind, "test")));

    // THEN
    assertThat(result.error()).contains("All models failed");
    // Verify only 5 attempts were made (MAX_MODEL_ATTEMPTS = 5)
    for (int i = 1; i <= 5; i++) {
      verify(modelExecutor).run(anyList(), anyString(), eq(models.get(i - 1)), anyList());
    }
    for (int i = 6; i <= 10; i++) {
      verify(modelExecutor, never()).run(anyList(), anyString(), eq(models.get(i - 1)), anyList());
    }
  }
}
