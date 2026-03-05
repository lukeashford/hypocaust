package com.example.hypocaust.tool.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.db.ToolEmbedding;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.repo.ToolEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for SemanticSearchToolRegistry.
 *
 * <p>The context includes real tool bean stubs defined below. Only infrastructure — repo,
 * embeddings, hashing — is mocked.
 */
@SpringBootTest(classes = {
    SemanticSearchToolRegistry.class,
    SemanticSearchToolRegistryTest.TestToolBeans.class
})
class SemanticSearchToolRegistryTest {

  @MockitoBean
  ToolEmbeddingRepository repository;
  @MockitoBean
  EmbeddingService embeddingService;
  @MockitoBean
  HashCalculator hashCalculator;

  @Autowired
  SemanticSearchToolRegistry registry;

  @Autowired
  ApplicationContext applicationContext;

  // ── Discovery ─────────────────────────────────────────────────────────────

  @Test
  void discoversAllMethodsAnnotatedWithBothAnnotations() {
    // simpleTool + enumTool + multiParamTool = 3
    // nonDiscoverableTool (@Tool only) must be excluded
    assertThat(registry.size()).isEqualTo(3);
  }

  @Test
  void doesNotDiscoverMethodsWithToolButWithoutDiscoverableTool() {
    assertThat(registry.getCallback("nonDiscoverableTool")).isEmpty();
  }

  @Test
  void doesNotDiscoverMethodsWithDiscoverableToolButWithoutTool() {
    assertThat(registry.getCallback("markerOnlyTool")).isEmpty();
  }

  // ── Callback & definition correctness ────────────────────────────────────

  @Test
  void getCallback_returnsCallbackWithCorrectNameAndDescription() {
    var cb = registry.getCallback("simpleTool").orElseThrow();

    assertThat(cb.getToolDefinition().name()).isEqualTo("simpleTool");
    assertThat(cb.getToolDefinition().description()).isEqualTo("A simple discoverable tool");
  }

  @Test
  void getCallback_unknownName_returnsEmpty() {
    assertThat(registry.getCallback("doesNotExist")).isEmpty();
  }

  @Test
  void toolDefinition_inputSchema_containsAllParameterNames() {
    var schema = registry.getCallback("multiParamTool").orElseThrow()
        .getToolDefinition().inputSchema();

    assertThat(schema).contains("\"type\" : \"object\"");
    assertThat(schema).contains("task");
    assertThat(schema).contains("count");
  }

  @Test
  void toolDefinition_enumParam_schemaContainsEnumValues() {
    var schema = registry.getCallback("enumTool").orElseThrow()
        .getToolDefinition().inputSchema();

    // Verifies ModelOptionsUtils correctly resolves ArtifactKind into its enum literals
    assertThat(schema).containsIgnoringCase("enum");
  }

  // ── Embedding lifecycle ───────────────────────────────────────────────────

  @Test
  void onFirstStartup_generatesAndPersistsEmbeddingForEachDiscoverableTool() {
    clearInvocations(embeddingService, repository);
    buildFreshRegistry().afterSingletonsInstantiated();

    // repository.findAll() returns empty by default → all 3 tools are new
    verify(embeddingService, atLeast(3)).generateEmbedding(anyString());
    verify(repository).saveAll(argThat(rows -> ((List<?>) rows).size() == 3));
  }

  @Test
  void whenHashIsUnchanged_doesNotRegenerateEmbedding() {
    when(hashCalculator.calculateSha256Hash(anyString())).thenReturn("stableHash");
    when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[1536]);
    when(repository.findAll()).thenReturn(List.of(
        ToolEmbedding.builder().toolName("simpleTool").embedding(new float[1536]).hash("stableHash")
            .build()
    ));

    // The shared registry already called generateEmbedding during context startup.
    // Clear that history so we only verify interactions from the fresh registry below.
    clearInvocations(embeddingService);
    buildFreshRegistry().afterSingletonsInstantiated();

    verify(embeddingService, never())
        .generateEmbedding(argThat(text -> text.contains("simpleTool")));
  }

  @Test
  void whenHashChanges_regeneratesEmbedding() {
    when(hashCalculator.calculateSha256Hash(anyString())).thenReturn("newHash");
    when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[1536]);
    when(repository.findAll()).thenReturn(List.of(
        ToolEmbedding.builder().toolName("simpleTool").embedding(new float[1536]).hash("oldHash")
            .build()
    ));

    buildFreshRegistry().afterSingletonsInstantiated();

    verify(embeddingService, atLeast(1))
        .generateEmbedding(argThat(text -> text.contains("simpleTool")));
  }

  @Test
  void obsoleteEmbeddingsAreDeletedOnStartup() {
    when(repository.findAll()).thenReturn(List.of(
        ToolEmbedding.builder().toolName("removedTool").embedding(new float[1536]).hash("h").build()
    ));

    buildFreshRegistry().afterSingletonsInstantiated();

    verify(repository).deleteAll(argThat(rows ->
        ((List<ToolEmbedding>) rows).stream()
            .anyMatch(e -> e.getToolName().equals("removedTool"))
    ));
  }

  // ── Semantic search ───────────────────────────────────────────────────────

  @Test
  void searchByTask_returnsDescriptorsMatchedByRepository() {
    float[] vec = new float[1536];
    when(embeddingService.generateEmbedding("run a simple task")).thenReturn(vec);
    when(repository.findTopByEmbeddingSimilarity(any(), any(Pageable.class)))
        .thenReturn(List.of(
            ToolEmbedding.builder().toolName("simpleTool").embedding(vec).hash("h").build()
        ));

    var results = registry.searchByTask("run a simple task");

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().name()).isEqualTo("simpleTool");
    assertThat(results.getFirst().description()).isEqualTo("A simple discoverable tool");
  }

  @Test
  void searchByTask_whenEmbeddingServiceThrows_returnsEmptyList() {
    when(embeddingService.generateEmbedding(anyString()))
        .thenThrow(new RuntimeException("service unavailable"));

    assertThat(registry.searchByTask("anything")).isEmpty();
  }

  @Test
  void searchByTask_whenRepositoryReturnsUnknownName_filtersItOut() {
    float[] vec = new float[1536];
    when(embeddingService.generateEmbedding(anyString())).thenReturn(vec);
    when(repository.findTopByEmbeddingSimilarity(any(), any(Pageable.class)))
        .thenReturn(List.of(
            ToolEmbedding.builder().toolName("phantomTool").embedding(vec).hash("x").build()
        ));

    assertThat(registry.searchByTask("anything")).isEmpty();
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private SemanticSearchToolRegistry buildFreshRegistry() {
    return new SemanticSearchToolRegistry(
        repository, embeddingService, hashCalculator, applicationContext
    );
  }

  // ── Test tool beans ───────────────────────────────────────────────────────

  @TestConfiguration
  static class TestToolBeans {

    @Bean
    SimpleToolBean simpleToolBean() {
      return new SimpleToolBean();
    }

    @Bean
    EnumToolBean enumToolBean() {
      return new EnumToolBean();
    }

    @Bean
    MultiParamToolBean multiParamToolBean() {
      return new MultiParamToolBean();
    }

    @Bean
    NonDiscoverableToolBean nonDiscoverableToolBean() {
      return new NonDiscoverableToolBean();
    }

    @Bean
    MarkerOnlyToolBean markerOnlyToolBean() {
      return new MarkerOnlyToolBean();
    }
  }

  static class SimpleToolBean {

    @DiscoverableTool
    @Tool(name = "simpleTool", description = "A simple discoverable tool")
    public String run(@ToolParam(description = "The task input") String task) {
      return "done";
    }
  }

  static class EnumToolBean {

    @DiscoverableTool
    @Tool(name = "enumTool", description = "A tool that takes an enum param")
    public String run(@ToolParam(description = "Kind of artifact") ArtifactKind kind) {
      return kind.name();
    }
  }

  static class MultiParamToolBean {

    @DiscoverableTool
    @Tool(name = "multiParamTool", description = "A tool with multiple params")
    public String run(
        @ToolParam(description = "The task to execute") String task,
        @ToolParam(description = "How many times") int count
    ) {
      return task.repeat(count);
    }
  }

  /**
   * Tool only — registry must ignore this.
   */
  static class NonDiscoverableToolBean {

    @Tool(name = "nonDiscoverableTool", description = "Should not be in registry")
    public String run(@ToolParam(description = "input") String input) {
      return input;
    }
  }

  /**
   * DiscoverableTool only, no @Tool — registry must filter this out.
   */
  static class MarkerOnlyToolBean {

    @DiscoverableTool
    public String run(String input) {
      return input;
    }
  }
}