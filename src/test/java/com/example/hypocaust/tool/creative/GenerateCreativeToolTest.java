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
import com.example.hypocaust.mapper.ArtifactMapper;
import com.example.hypocaust.models.ExecutionResult;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.models.ModelExecutor;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.SearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GenerateCreativeToolTest {

  private ModelEmbeddingRegistry modelRag;
  private ModelExecutor modelExecutor;
  private WordingService wordingService;
  private ObjectMapper objectMapper;
  private ArtifactMapper artifactMapper;
  private GenerateCreativeTool tool;

  private ArtifactsContext artifactsContext;

  @BeforeEach
  void setUp() {
    modelRag = mock(ModelEmbeddingRegistry.class);
    ExecutionRouter executionRouter = mock(ExecutionRouter.class);
    modelExecutor = mock(ModelExecutor.class);
    wordingService = mock(WordingService.class);
    objectMapper = new ObjectMapper();
    artifactMapper = mock(ArtifactMapper.class);
    tool = new GenerateCreativeTool(modelRag, executionRouter,
        wordingService, objectMapper, artifactMapper);

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

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, "balanced", task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("SDXL", "stability-ai", "sdxl", "A high-quality image model",
            "Use clear prompts", "balanced", "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Cute Cat");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn(
        "A very cute cat illustration");
    when(artifactsContext.add(any())).thenReturn("cute-cat-1");

    var providerInput = objectMapper.createObjectNode().put("prompt", "a cute cat");
    var finalizedArtifact = Artifact.builder()
        .name("cute-cat-1").kind(kind).title("Cute Cat")
        .description("A very cute cat illustration")
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/hash.png")
        .mimeType("image/png")
        .build();

    when(modelExecutor.run(any(Artifact.class), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(new ExecutionResult(finalizedArtifact, providerInput));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactName()).isEqualTo("cute-cat-1");
    assertThat(result.summary()).contains("Generated IMAGE using SDXL");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.name().equals("cute-cat-1") &&
            artifact.storageKey().equals("blobs/ab/cd/hash.png") &&
            artifact.status().equals(ArtifactStatus.MANIFESTED)
    ));
  }

  @Test
  void generate_noModelFound_returnsError() {
    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), ArtifactKind.IMAGE, "balanced",
            "Generate a sunset image"));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of());

    var result = tool.generate("Generate a sunset image", ArtifactKind.IMAGE);

    assertThat(result.error()).contains("No suitable model found");
    assertThat(result.artifactName()).isNull();
  }

  @Test
  void generate_planReturnsErrorMessage_fallsThrough() {
    // GIVEN — only one model candidate, plan fails → all models exhausted
    String task = "Make a video";
    ArtifactKind kind = ArtifactKind.VIDEO;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, "balanced", task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(
        List.of(new SearchResult("AnimateDiff", "lucataco", "animate-diff", "A video model",
            "Keep it short", "balanced", "REPLICATE", Set.of(ArtifactKind.TEXT),
            Set.of(ArtifactKind.VIDEO))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Video");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A video");
    when(artifactsContext.add(any())).thenReturn("video-1");

    when(modelExecutor.run(any(Artifact.class), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("Planning failed: Missing video length"));

    // WHEN
    var result = tool.generate(task, kind);

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
        .thenReturn(new ModelRequirement(Set.of(), kind, "balanced", task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("SDXL", "stability-ai", "sdxl", "desc", "best", "balanced", "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Cat");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A cat");
    when(artifactsContext.add(any())).thenReturn("cat-1");

    when(modelExecutor.run(any(Artifact.class), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("Provider API timeout"));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("All models failed");
    verify(artifactsContext).rollbackPending("cat-1");
  }

  @Test
  void generate_nullOutputUrl_rollsBackAndFails() {
    // GIVEN — executor throws when output is "null"
    String task = "Make something";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, "balanced", task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("SDXL", "stability-ai", "sdxl", "desc", "best", "balanced", "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Thing");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A thing");
    when(artifactsContext.add(any())).thenReturn("thing-1");

    when(modelExecutor.run(any(Artifact.class), anyString(), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenThrow(new IllegalStateException("Model returned no usable output"));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("All models failed");
    verify(artifactsContext).rollbackPending("thing-1");
  }

  @Test
  void generate_text_inlineContent() {
    // GIVEN
    String task = "Write a poem";
    ArtifactKind kind = ArtifactKind.TEXT;

    when(wordingService.generateModelRequirement(anyString(), eq(kind)))
        .thenReturn(new ModelRequirement(Set.of(), kind, "high", task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("Claude Opus", "anthropic", "claude-3-opus", "desc", "best", "high",
            "OPENROUTER", Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.TEXT))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Poem");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A poem");
    when(artifactsContext.add(any())).thenReturn("poem-1");

    String poemText = "Roses are red...";
    var providerInput = objectMapper.createObjectNode().put("prompt", "poem");
    var finalizedArtifact = Artifact.builder()
        .name("poem-1").kind(kind).title("Poem").description("A poem")
        .status(ArtifactStatus.MANIFESTED)
        .inlineContent(new TextNode(poemText))
        .mimeType("text/plain")
        .build();

    when(modelExecutor.run(any(Artifact.class), eq(task), anyString(), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(new ExecutionResult(finalizedArtifact, providerInput));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactName()).isEqualTo("poem-1");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.kind() == ArtifactKind.TEXT &&
            artifact.storageKey() == null &&
            artifact.inlineContent().asText().equals(poemText) &&
            artifact.status() == ArtifactStatus.MANIFESTED
    ));
  }

  @Test
  void generate_fallbackToSecondModel_onFirstModelFailure() {
    // GIVEN — two models returned by RAG, first fails technically, second succeeds
    String task = "Make a sunset";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, "balanced", task));

    var model1 = new SearchResult("FluxDev", "black-forest-labs", "flux-dev",
        "desc1", "best1", "balanced", "REPLICATE",
        Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE));
    var model2 = new SearchResult("SDXL", "stability-ai", "sdxl",
        "desc2", "best2", "balanced", "REPLICATE",
        Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE));

    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(model1, model2));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Sunset");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A sunset");
    when(artifactsContext.add(any())).thenReturn("sunset-1", "sunset-2");

    // First model: run fails
    when(modelExecutor.run(any(Artifact.class), anyString(), eq("FluxDev"), anyString(),
        anyString(), anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("Model unavailable"));

    // Second model: run succeeds
    var providerInput = objectMapper.createObjectNode().put("prompt", "sunset sdxl");
    var finalizedArtifact = Artifact.builder()
        .name("sunset-2").kind(kind).title("Sunset").description("A sunset")
        .status(ArtifactStatus.MANIFESTED)
        .storageKey("blobs/ab/cd/sunset.png")
        .mimeType("image/png")
        .build();

    when(modelExecutor.run(any(Artifact.class), anyString(), eq("SDXL"), anyString(), anyString(),
        anyString(), anyString(), any()))
        .thenReturn(new ExecutionResult(finalizedArtifact, providerInput));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN — succeeded with second model
    assertThat(result.error()).isNull();
    assertThat(result.summary()).contains("Generated IMAGE using SDXL");

    // First artifact should have been rolled back
    verify(artifactsContext).rollbackPending("sunset-1");
    // Second artifact should have been updated
    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.storageKey().equals("blobs/ab/cd/sunset.png")));
  }

  @Nested
  class SubstituteArtifacts {

    @Test
    void replacesPlaceholderWithPresignedUrl() throws Exception {
      var input = objectMapper.readTree("{\"image\": \"@photo\"}");
      var artifact = Artifact.builder()
          .name("photo").kind(ArtifactKind.IMAGE).title("T").description("D")
          .status(ArtifactStatus.MANIFESTED).storageKey("blobs/ab/cd/photo.png").build();

      when(artifactMapper.toPresignedUrl("blobs/ab/cd/photo.png"))
          .thenReturn("https://cdn.example.com/presigned/photo.png");

      var result = tool.substituteArtifacts(input, List.of(artifact));
      assertThat(result.get("image").asText()).isEqualTo(
          "https://cdn.example.com/presigned/photo.png");
    }

    @Test
    void nullStorageKey_leavesPlaceholderAsIs() throws Exception {
      var input = objectMapper.readTree("{\"image\": \"@photo\"}");
      var artifact = Artifact.builder()
          .name("photo").kind(ArtifactKind.IMAGE).title("T").description("D")
          .status(ArtifactStatus.GESTATING).build();

      var result = tool.substituteArtifacts(input, List.of(artifact));
      assertThat(result.get("image").asText()).isEqualTo("@photo");
    }
  }
}
