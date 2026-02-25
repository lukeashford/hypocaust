package com.example.hypocaust.tool.creative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.agent.TaskExecutionContextHolder;
import com.example.hypocaust.domain.Artifact;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.models.ExecutionAttempt;
import com.example.hypocaust.models.ExecutionPlan;
import com.example.hypocaust.models.ExecutionRouter;
import com.example.hypocaust.models.ModelExecutor;
import com.example.hypocaust.rag.ModelEmbeddingRegistry;
import com.example.hypocaust.rag.ModelEmbeddingRegistry.SearchResult;
import com.example.hypocaust.rag.ModelRequirement;
import com.example.hypocaust.service.WordingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
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

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, tier, task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("SDXL", owner, modelId, description, bestPractices, tier, "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    var planInput = objectMapper.createObjectNode().put("prompt", "a cute cat");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());

    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Cute Cat");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn(
        "A very cute cat illustration");

    when(artifactsContext.add(any())).thenReturn("cute-cat-1");

    JsonNode executorOutput = objectMapper.valueToTree("https://replicate.com/output.png");
    when(modelExecutor.executeWithRetry(eq("stability-ai"), eq("sdxl"), any()))
        .thenReturn(ExecutionAttempt.success(executorOutput, List.of(
            Map.of("attempt", "1", "platform", "REPLICATE", "model", "stability-ai/sdxl",
                "status", "success"))));
    when(modelExecutor.extractOutput(executorOutput)).thenReturn(
        "https://replicate.com/output.png");

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactName()).isEqualTo("cute-cat-1");
    assertThat(result.summary()).contains("Generated IMAGE using SDXL");
    assertThat(result.report()).isNotNull();
    assertThat(result.report().success()).isTrue();
    assertThat(result.report().artifactNames()).contains("cute-cat-1");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.name().equals("cute-cat-1") &&
            artifact.url().equals("https://replicate.com/output.png") &&
            artifact.status().equals(ArtifactStatus.CREATED)
    ));
  }

  @Test
  void generate_noModelFound_returnsError() {
    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), ArtifactKind.IMAGE, "balanced",
            "Generate a sunset image"));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of());

    var result = tool.generate("Generate a sunset image", ArtifactKind.IMAGE);

    assertThat(result.error()).contains("No suitable model found matching requirements");
    assertThat(result.artifactName()).isNull();
    assertThat(result.report()).isNotNull();
    assertThat(result.report().success()).isFalse();
  }

  @Test
  void generate_planReturnsErrorMessage_fallsThrough() {
    // GIVEN — only one model candidate, plan fails → all models exhausted
    String task = "Make a video";
    ArtifactKind kind = ArtifactKind.VIDEO;
    String owner = "lucataco";
    String modelId = "animate-diff";
    String tier = "balanced";

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, tier, task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(
        List.of(new SearchResult("AnimateDiff", owner, modelId, "A video model",
            "Keep it short", tier, "REPLICATE", Set.of(ArtifactKind.TEXT),
            Set.of(ArtifactKind.VIDEO))));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(ExecutionPlan.error("Missing video length"));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN — falls through as all candidates exhausted
    assertThat(result.error()).contains("candidate models failed");
    assertThat(result.report()).isNotNull();
    assertThat(result.report().attempts()).isNotEmpty();
  }

  @Test
  void generate_executorCallFails_rollsBackArtifact() {
    // GIVEN
    String task = "Make a cat";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String owner = "stability-ai";
    String modelId = "sdxl";
    String tier = "balanced";

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, tier, task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("SDXL", owner, modelId, "desc", "best", tier, "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    var planInput = objectMapper.createObjectNode().put("prompt", "cat");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Cat");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A cat");
    when(artifactsContext.add(any())).thenReturn("cat-1");

    when(modelExecutor.executeWithRetry(anyString(), anyString(), any()))
        .thenReturn(ExecutionAttempt.failure(List.of(
            Map.of("attempt", "1", "status", "failed", "error", "Provider API timeout",
                "errorType", "RuntimeException", "transient", "false"))));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("candidate models failed");
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

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, tier, task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("SDXL", owner, modelId, "desc", "best", tier, "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    var planInput = objectMapper.createObjectNode().put("prompt", "thing");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Thing");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A thing");
    when(artifactsContext.add(any())).thenReturn("thing-1");

    JsonNode nullOutput = objectMapper.nullNode();
    when(modelExecutor.executeWithRetry(anyString(), anyString(), any()))
        .thenReturn(ExecutionAttempt.success(nullOutput, List.of(
            Map.of("attempt", "1", "status", "success"))));
    when(modelExecutor.extractOutput(nullOutput)).thenReturn("null");

    // WHEN
    var result = tool.generate(task, kind);

    // THEN — extract_output failure, all candidates exhausted
    assertThat(result.error()).contains("candidate models failed");
    verify(artifactsContext).rollbackPending("thing-1");
  }

  @Test
  void generate_text_inlineContent() {
    // GIVEN
    String task = "Write a poem";
    ArtifactKind kind = ArtifactKind.TEXT;
    String owner = "anthropic";
    String modelId = "claude-3-opus";
    String tier = "high";

    when(wordingService.generateModelRequirement(anyString(), eq(kind)))
        .thenReturn(new ModelRequirement(Set.of(), kind, tier, task));
    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(
        new SearchResult("Claude Opus", owner, modelId, "desc", "best", tier, "OPENROUTER",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.TEXT))));

    var planInput = objectMapper.createObjectNode().put("prompt", "poem");
    when(modelExecutor.generatePlan(anyString(), eq(kind), anyString(), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Poem");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A poem");
    when(artifactsContext.add(any())).thenReturn("poem-1");

    String poemText = "Roses are red...";
    JsonNode output = objectMapper.createObjectNode().set("choices",
        objectMapper.createArrayNode().add(
            objectMapper.createObjectNode().set("message",
                objectMapper.createObjectNode().put("content", poemText))));

    when(modelExecutor.executeWithRetry(anyString(), anyString(), any()))
        .thenReturn(ExecutionAttempt.success(output, List.of(
            Map.of("attempt", "1", "status", "success"))));
    when(modelExecutor.extractOutput(output)).thenReturn(poemText);

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isNull();
    assertThat(result.artifactName()).isEqualTo("poem-1");

    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.kind() == ArtifactKind.TEXT &&
            artifact.url() == null &&
            artifact.inlineContent().asText().equals(poemText) &&
            artifact.status() == ArtifactStatus.MANIFESTED
    ));
  }

  @Test
  void generate_fallbackToSecondModel_onFirstModelFailure() {
    // GIVEN — two models returned by RAG, first fails technically, second succeeds
    String task = "Make a sunset";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String tier = "balanced";

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, tier, task));

    var model1 = new SearchResult("FluxDev", "black-forest-labs", "flux-dev",
        "desc1", "best1", tier, "REPLICATE",
        Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE));
    var model2 = new SearchResult("SDXL", "stability-ai", "sdxl",
        "desc2", "best2", tier, "REPLICATE",
        Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE));

    when(modelRag.search(any(ModelRequirement.class))).thenReturn(List.of(model1, model2));

    // First model: plan OK, execution fails
    var planInput1 = objectMapper.createObjectNode().put("prompt", "sunset flux");
    when(modelExecutor.generatePlan(anyString(), any(), eq("FluxDev"), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(new ExecutionPlan(planInput1, null));

    // Second model: plan OK, execution succeeds
    var planInput2 = objectMapper.createObjectNode().put("prompt", "sunset sdxl");
    when(modelExecutor.generatePlan(anyString(), any(), eq("SDXL"), anyString(), anyString(),
        anyString(), anyString()))
        .thenReturn(new ExecutionPlan(planInput2, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Sunset");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("A sunset");
    when(artifactsContext.add(any())).thenReturn("sunset-1", "sunset-2");

    // First executeWithRetry fails
    when(modelExecutor.executeWithRetry(eq("black-forest-labs"), eq("flux-dev"), any()))
        .thenReturn(ExecutionAttempt.failure(List.of(
            Map.of("attempt", "1", "status", "failed", "error", "Model unavailable"))));

    // Second executeWithRetry succeeds
    JsonNode successOutput = objectMapper.valueToTree("https://replicate.com/sunset.png");
    when(modelExecutor.executeWithRetry(eq("stability-ai"), eq("sdxl"), any()))
        .thenReturn(ExecutionAttempt.success(successOutput, List.of(
            Map.of("attempt", "1", "status", "success"))));
    when(modelExecutor.extractOutput(successOutput)).thenReturn("https://replicate.com/sunset.png");

    // WHEN
    var result = tool.generate(task, kind);

    // THEN — succeeded with second model
    assertThat(result.error()).isNull();
    assertThat(result.summary()).contains("Generated IMAGE using SDXL");
    assertThat(result.report()).isNotNull();
    assertThat(result.report().success()).isTrue();
    // Report should contain attempts from both models
    assertThat(result.report().attempts().size()).isGreaterThanOrEqualTo(2);

    // First artifact should have been rolled back
    verify(artifactsContext).rollbackPending("sunset-1");
    // Second artifact should have been updated
    verify(artifactsContext).updatePending(argThat(artifact ->
        artifact.url().equals("https://replicate.com/sunset.png")));
  }

  @Test
  void generate_reportContainsAttemptMetadata() {
    // GIVEN
    String task = "Make art";
    ArtifactKind kind = ArtifactKind.IMAGE;

    when(wordingService.generateModelRequirement(anyString(), any()))
        .thenReturn(new ModelRequirement(Set.of(), kind, "balanced", task));
    when(modelRag.search(any())).thenReturn(List.of(
        new SearchResult("SDXL", "stability-ai", "sdxl", "d", "b", "balanced", "REPLICATE",
            Set.of(ArtifactKind.TEXT), Set.of(ArtifactKind.IMAGE))));

    var planInput = objectMapper.createObjectNode().put("prompt", "art");
    when(modelExecutor.generatePlan(anyString(), any(), anyString(), anyString(), anyString(),
        anyString(), anyString())).thenReturn(new ExecutionPlan(planInput, null));

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(wordingService.generateArtifactTitle(anyString())).thenReturn("Art");
    when(wordingService.generateArtifactDescription(anyString())).thenReturn("Art");
    when(artifactsContext.add(any())).thenReturn("art-1");

    JsonNode output = objectMapper.valueToTree("https://cdn.example.com/art.png");
    when(modelExecutor.executeWithRetry(anyString(), anyString(), any()))
        .thenReturn(ExecutionAttempt.success(output, List.of(
            Map.of("attempt", "1", "platform", "REPLICATE",
                "model", "stability-ai/sdxl", "status", "success"))));
    when(modelExecutor.extractOutput(output)).thenReturn("https://cdn.example.com/art.png");

    // WHEN
    var result = tool.generate(task, kind);

    // THEN — report has structured attempt data
    assertThat(result.report()).isNotNull();
    assertThat(result.report().success()).isTrue();
    assertThat(result.report().attempts()).isNotEmpty();
    var firstAttempt = result.report().attempts().getFirst();
    assertThat(firstAttempt).containsKey("step");
    assertThat(firstAttempt).containsEntry("status", "success");
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
