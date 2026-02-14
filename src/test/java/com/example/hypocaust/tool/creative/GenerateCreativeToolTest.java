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
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
import com.example.hypocaust.domain.ArtifactsContext;
import com.example.hypocaust.domain.TaskExecutionContext;
import com.example.hypocaust.integration.ReplicateClient;
import com.example.hypocaust.models.ModelRegistry;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.rag.PlatformEmbeddingRegistry;
import com.example.hypocaust.rag.PlatformEmbeddingRegistry.SearchResult;
import com.example.hypocaust.domain.Artifact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class GenerateCreativeToolTest {

  private PlatformEmbeddingRegistry modelRag;
  private ModelRegistry modelRegistry;
  private ReplicateClient replicateClient;
  private ObjectMapper objectMapper;
  private GenerateCreativeTool tool;

  private ArtifactsContext artifactsContext;
  private AnthropicChatModel chatModel;

  @BeforeEach
  void setUp() {
    modelRag = mock(PlatformEmbeddingRegistry.class);
    modelRegistry = mock(ModelRegistry.class);
    replicateClient = mock(ReplicateClient.class);
    objectMapper = new ObjectMapper();
    tool = new GenerateCreativeTool(modelRag, modelRegistry, replicateClient, objectMapper);

    TaskExecutionContext context = mock(TaskExecutionContext.class);
    when(context.getTaskExecutionId()).thenReturn(java.util.UUID.randomUUID());
    artifactsContext = mock(ArtifactsContext.class);
    when(context.getArtifacts()).thenReturn(artifactsContext);
    TaskExecutionContextHolder.setContext(context);

    chatModel = mock(AnthropicChatModel.class);
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
    String modelDoc = "Replicate: stability-ai/sdxl\nVersion: latest-hash";

    when(modelRag.search(anyString())).thenReturn(List.of(new SearchResult("SDXL", modelDoc)));

    ObjectNode modelVersion = objectMapper.createObjectNode();
    modelVersion.set("openapi_schema", objectMapper.createObjectNode());
    when(replicateClient.getModelVersion("stability-ai", "sdxl", "latest-hash")).thenReturn(
        modelVersion);

    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());

    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(chatModel);

    String planJson = """
        {
          "title": "Cute Cat",
          "description": "A very cute cat illustration",
          "replicateInput": {"prompt": "a cute cat"},
          "errorMessage": null
        }
        """;
    AssistantMessage assistantMessage = new AssistantMessage(planJson);
    Generation generation = new Generation(assistantMessage);
    ChatResponse chatResponse = new ChatResponse(List.of(generation));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    when(artifactsContext.add(any())).thenReturn("cute-cat-1");

    JsonNode replicateOutput = objectMapper.valueToTree("https://replicate.com/output.png");
    when(replicateClient.predict(eq("stability-ai"), eq("sdxl"), eq("latest-hash"),
        any())).thenReturn(replicateOutput);

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
    String modelDoc = "Replicate: lucataco/animate-diff\nVersion: v1";

    when(modelRag.search(anyString())).thenReturn(
        List.of(new SearchResult("AnimateDiff", modelDoc)));

    ObjectNode modelVersion = objectMapper.createObjectNode();
    modelVersion.set("openapi_schema", objectMapper.createObjectNode());
    when(replicateClient.getModelVersion(anyString(), anyString(), anyString())).thenReturn(
        modelVersion);
    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(chatModel);

    String planJson = "{\"title\":null, \"description\":null, \"replicateInput\":null, \"errorMessage\":\"Missing video length\"}";
    AssistantMessage assistantMessage = new AssistantMessage(planJson);
    Generation generation = new Generation(assistantMessage);
    ChatResponse chatResponse = new ChatResponse(List.of(generation));
    when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).isEqualTo("Missing video length");
  }

  @Test
  void generate_replicateCallFails_rollsBackArtifact() {
    // GIVEN
    String task = "Make a cat";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String modelDoc = "Replicate: stability-ai/sdxl\nVersion: v1";

    when(modelRag.search(anyString())).thenReturn(List.of(new SearchResult("SDXL", modelDoc)));

    ObjectNode modelVersion = objectMapper.createObjectNode();
    modelVersion.set("openapi_schema", objectMapper.createObjectNode());
    when(replicateClient.getModelVersion(anyString(), anyString(), anyString()))
        .thenReturn(modelVersion);
    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(chatModel);

    String planJson = """
        {"title": "Cat", "description": "A cat", "replicateInput": {"prompt": "cat"}, "errorMessage": null}
        """;
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(planJson)))));
    when(artifactsContext.add(any())).thenReturn("cat-1");

    when(replicateClient.predict(anyString(), anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("Replicate API timeout"));

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("Replicate API timeout");
    assertThat(result.artifactName()).isNull();
    verify(artifactsContext).rollbackPending("cat-1");
  }

  @Test
  void generate_nullOutputUrl_returnsError() {
    // GIVEN — Replicate returns a null node as output
    String task = "Make something";
    ArtifactKind kind = ArtifactKind.IMAGE;
    String modelDoc = "Replicate: stability-ai/sdxl\nVersion: v1";

    when(modelRag.search(anyString())).thenReturn(List.of(new SearchResult("SDXL", modelDoc)));

    ObjectNode modelVersion = objectMapper.createObjectNode();
    modelVersion.set("openapi_schema", objectMapper.createObjectNode());
    when(replicateClient.getModelVersion(anyString(), anyString(), anyString()))
        .thenReturn(modelVersion);
    when(artifactsContext.getAllWithChanges()).thenReturn(List.of());
    when(modelRegistry.get(any(AnthropicChatModelSpec.class))).thenReturn(chatModel);

    String planJson = """
        {"title": "Thing", "description": "A thing", "replicateInput": {"prompt": "thing"}, "errorMessage": null}
        """;
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(planJson)))));
    when(artifactsContext.add(any())).thenReturn("thing-1");

    // Replicate returns a NullNode (output field is null)
    JsonNode nullOutput = objectMapper.nullNode();
    when(replicateClient.predict(anyString(), anyString(), anyString(), any()))
        .thenReturn(nullOutput);

    // WHEN
    var result = tool.generate(task, kind);

    // THEN
    assertThat(result.error()).contains("no usable output URL");
    verify(artifactsContext).rollbackPending("thing-1");
  }

  @Nested
  class ExtractOutputUrl {

    @Test
    void textualNode_returnsText() {
      var node = objectMapper.valueToTree("https://example.com/img.png");
      assertThat(tool.extractOutputUrl(node)).isEqualTo("https://example.com/img.png");
    }

    @Test
    void arrayNode_returnsFirstElement() {
      var node = objectMapper.valueToTree(List.of("https://example.com/1.png", "https://example.com/2.png"));
      assertThat(tool.extractOutputUrl(node)).isEqualTo("https://example.com/1.png");
    }

    @Test
    void objectWithUrlField_returnsUrlValue() throws Exception {
      var node = objectMapper.readTree("{\"url\": \"https://example.com/out.png\", \"other\": 42}");
      assertThat(tool.extractOutputUrl(node)).isEqualTo("https://example.com/out.png");
    }

    @Test
    void otherShape_fallsBackToToString() throws Exception {
      var node = objectMapper.readTree("{\"data\": 123}");
      assertThat(tool.extractOutputUrl(node)).isEqualTo("{\"data\":123}");
    }
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

  @Nested
  class ParseModelInfo {

    @Test
    void standardFormat_parsesCorrectly() {
      var info = tool.parseModelInfo(
          "Some docs\nReplicate: acme/super-model\nVersion: abc123\nMore text");

      assertThat(info.owner()).isEqualTo("acme");
      assertThat(info.name()).isEqualTo("super-model");
      assertThat(info.version()).isEqualTo("abc123");
    }
  }
}
