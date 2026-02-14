package com.example.hypocaust.tool.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.repo.ToolEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;

class SemanticSearchToolRegistryTest {

  private ToolEmbeddingRepository repository;
  private EmbeddingService embeddingService;
  private HashCalculator hashCalculator;
  private ApplicationContext applicationContext;

  @BeforeEach
  void setUp() {
    repository = mock(ToolEmbeddingRepository.class);
    embeddingService = mock(EmbeddingService.class);
    hashCalculator = mock(HashCalculator.class);
    applicationContext = mock(ApplicationContext.class);

    when(hashCalculator.calculateSha256Hash(anyString())).thenReturn("hash123");
    when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[1536]);
    when(repository.findByToolName(anyString())).thenReturn(Optional.empty());
  }

  @Test
  void initialize_discoversAnnotatedBeans() {
    var testTool = new TestToolBean();
    when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"testTool"});
    when(applicationContext.getBean("testTool")).thenReturn(testTool);
    when(applicationContext.getType("testTool")).thenReturn((Class) TestToolBean.class);

    var registry = new SemanticSearchToolRegistry(
        repository, embeddingService, hashCalculator, applicationContext);
    registry.afterSingletonsInstantiated();

    assertThat(registry.size()).isEqualTo(1);
    assertThat(registry.getCallback("test_tool")).isPresent();
  }

  @Test
  void initialize_discoversClassLevelAnnotation() {
    var classTool = new ClassLevelToolBean();
    when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"classTool"});
    when(applicationContext.getBean("classTool")).thenReturn(classTool);
    when(applicationContext.getType("classTool")).thenReturn((Class) ClassLevelToolBean.class);

    var registry = new SemanticSearchToolRegistry(
        repository, embeddingService, hashCalculator, applicationContext);
    registry.afterSingletonsInstantiated();

    assertThat(registry.size()).isEqualTo(1);
    assertThat(registry.getCallback("class_tool")).isPresent();
    assertThat(registry.getCallback("class_tool").get().getToolDefinition().description())
        .isEqualTo("Class level description");
  }

  @Test
  void initialize_buildsCorrectParameterSchema() {
    var testTool = new TestToolBean();
    when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"testTool"});
    when(applicationContext.getBean("testTool")).thenReturn(testTool);
    when(applicationContext.getType("testTool")).thenReturn((Class) TestToolBean.class);

    var registry = new SemanticSearchToolRegistry(
        repository, embeddingService, hashCalculator, applicationContext);
    registry.afterSingletonsInstantiated();

    var callback = registry.getCallback("test_tool");
    assertThat(callback).isPresent();

    var schema = callback.get().getToolDefinition().inputSchema();
    assertThat(schema).contains("\"type\":\"string\"");
    assertThat(schema).contains("\"description\":");
  }

  @Test
  void initialize_enumParameterIncludesEnumValues() {
    var enumTool = new EnumToolBean();
    when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"enumTool"});
    when(applicationContext.getBean("enumTool")).thenReturn(enumTool);
    when(applicationContext.getType("enumTool")).thenReturn((Class) EnumToolBean.class);

    var registry = new SemanticSearchToolRegistry(
        repository, embeddingService, hashCalculator, applicationContext);
    registry.afterSingletonsInstantiated();

    var callback = registry.getCallback("enum_tool");
    assertThat(callback).isPresent();

    var schema = callback.get().getToolDefinition().inputSchema();
    assertThat(schema).contains("\"enum\":");
    assertThat(schema).contains("STRUCTURED_JSON");
    assertThat(schema).contains("IMAGE");
    assertThat(schema).contains("AUDIO");
    assertThat(schema).contains("VIDEO");
  }

  @Test
  void getCallback_unknownTool_returnsEmpty() {
    when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{});

    var registry = new SemanticSearchToolRegistry(
        repository, embeddingService, hashCalculator, applicationContext);
    registry.afterSingletonsInstantiated();

    assertThat(registry.getCallback("nonexistent")).isEmpty();
  }

  // Test helper beans
  @DiscoverableTool(name = "test_tool", description = "A test tool")
  static class TestToolBean {

    @DiscoverableTool(name = "test_tool", description = "Execute test")
    public String run(
        @ToolParam(description = "The task to run") String task
    ) {
      return "ok";
    }
  }

  @DiscoverableTool(name = "class_tool", description = "Class level description")
  static class ClassLevelToolBean {

    public String run(
        @ToolParam(description = "The task to run") String task
    ) {
      return "ok";
    }
  }

  @DiscoverableTool(name = "enum_tool", description = "A tool with enum param")
  static class EnumToolBean {

    @DiscoverableTool(name = "enum_tool", description = "Execute with kind")
    public String run(
        @ToolParam(description = "Kind of artifact") ArtifactKind kind
    ) {
      return "ok";
    }
  }
}
