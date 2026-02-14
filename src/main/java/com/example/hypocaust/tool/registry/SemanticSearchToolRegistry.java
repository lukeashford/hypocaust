package com.example.hypocaust.tool.registry;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.db.ToolEmbedding;
import com.example.hypocaust.repo.ToolEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Semantic search-enabled tool registry. Discovers all {@link DiscoverableTool}-annotated
 * beans, generates embeddings for their descriptions, and provides semantic search.
 *
 * <p>Preserves the same elegant auto-registration pattern as the former
 * {@code SemanticSearchOperatorRegistry}: just write a new tool class with the annotation
 * and Spring picks it up.
 */
@Component
@Slf4j
public class SemanticSearchToolRegistry implements ToolRegistry {

  private static final int MAX_RESULTS = 3;

  private final ToolEmbeddingRepository repository;
  private final EmbeddingService embeddingService;
  private final HashCalculator hashCalculator;
  private final ApplicationContext applicationContext;

  private final Map<String, ToolCallback> callbacksByName = new ConcurrentHashMap<>();
  private final Map<String, ToolDescriptor> descriptorsByName = new ConcurrentHashMap<>();

  public SemanticSearchToolRegistry(
      ToolEmbeddingRepository repository,
      EmbeddingService embeddingService,
      HashCalculator hashCalculator,
      ApplicationContext applicationContext
  ) {
    this.repository = repository;
    this.embeddingService = embeddingService;
    this.hashCalculator = hashCalculator;
    this.applicationContext = applicationContext;
  }

  @PostConstruct
  public void initialize() {
    discoverAndIndexTools();
  }

  private void discoverAndIndexTools() {
    log.info("Discovering and indexing tools...");

    var beans = applicationContext.getBeansWithAnnotation(DiscoverableTool.class);
    var upsertRows = new ArrayList<ToolEmbedding>();
    var currentToolNames = new HashSet<String>();

    for (var entry : beans.entrySet()) {
      var bean = entry.getValue();
      var annotation = bean.getClass().getAnnotation(DiscoverableTool.class);
      if (annotation == null) {
        continue;
      }

      var toolName = annotation.name();
      var toolDescription = annotation.description();
      currentToolNames.add(toolName);

      // Find the @Tool-annotated method
      Method toolMethod = findToolMethod(bean);
      if (toolMethod == null) {
        log.warn("No @Tool method found on {}", bean.getClass().getSimpleName());
        continue;
      }

      // Build ToolCallback using Spring AI's infrastructure
      var callback = MethodToolCallback.builder()
          .toolDefinition(org.springframework.ai.tool.definition.ToolDefinition.builder()
              .name(toolName)
              .description(toolDescription)
              .inputSchema(buildParameterSchema(toolMethod))
              .build())
          .toolMethod(toolMethod)
          .toolObject(bean)
          .build();

      callbacksByName.put(toolName, callback);

      // Build descriptor for search results
      var descriptor = new ToolDescriptor(
          toolName,
          toolDescription,
          buildParameterSchema(toolMethod)
      );
      descriptorsByName.put(toolName, descriptor);

      // Generate embedding
      try {
        var embeddingText = createEmbeddingText(toolName, toolDescription, toolMethod);
        var textHash = hashCalculator.calculateSha256Hash(embeddingText);
        var existingOpt = repository.findByToolName(toolName);

        if (existingOpt.isEmpty()) {
          upsertRows.add(ToolEmbedding.builder()
              .toolName(toolName)
              .embedding(embeddingService.generateEmbedding(embeddingText))
              .hash(textHash)
              .build());
        } else if (!Objects.equals(existingOpt.get().getHash(), textHash)) {
          var existing = existingOpt.get();
          existing.updateEmbedding(embeddingService.generateEmbedding(embeddingText), textHash);
          upsertRows.add(existing);
        }
      } catch (Exception e) {
        log.error("Failed to process tool {}: {}", toolName, e.getMessage(), e);
      }
    }

    if (!upsertRows.isEmpty()) {
      repository.saveAll(upsertRows);
      log.info("Generated embeddings for {} tools", upsertRows.size());
    }

    // Clean up obsolete embeddings
    try {
      var obsolete = repository.findAll().stream()
          .filter(e -> !currentToolNames.contains(e.getToolName()))
          .toList();
      if (!obsolete.isEmpty()) {
        repository.deleteAll(obsolete);
        log.info("Deleted {} obsolete tool embeddings", obsolete.size());
      }
    } catch (Exception e) {
      log.warn("Failed to clean up obsolete tool embeddings: {}", e.getMessage(), e);
    }

    log.info("Tool registry initialized with {} tools", callbacksByName.size());
  }

  private Method findToolMethod(Object bean) {
    for (var method : bean.getClass().getMethods()) {
      if (method.isAnnotationPresent(Tool.class)) {
        return method;
      }
    }
    return null;
  }

  private String createEmbeddingText(String name, String description, Method method) {
    var sb = new StringBuilder();
    sb.append("Tool: ").append(name);
    if (description != null && !description.isBlank()) {
      sb.append(" - ").append(description);
    }
    // Add parameter names for better matching
    var params = method.getParameters();
    if (params.length > 0) {
      sb.append(" | Parameters: ");
      for (int i = 0; i < params.length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(params[i].getName());
      }
    }
    return sb.toString();
  }

  private String buildParameterSchema(Method method) {
    // Build a simple JSON schema from the method parameters
    var sb = new StringBuilder("{\"type\":\"object\",\"properties\":{");
    var params = method.getParameters();
    for (int i = 0; i < params.length; i++) {
      if (i > 0) {
        sb.append(",");
      }
      var param = params[i];
      var toolParam = param.getAnnotation(
          org.springframework.ai.tool.annotation.ToolParam.class);
      sb.append("\"").append(param.getName()).append("\":{\"type\":\"string\"");
      if (toolParam != null && !toolParam.description().isEmpty()) {
        sb.append(",\"description\":\"")
            .append(toolParam.description().replace("\"", "\\\""))
            .append("\"");
      }
      sb.append("}");
    }
    sb.append("}}");
    return sb.toString();
  }

  @Override
  public List<ToolDescriptor> searchByTask(String taskDescription) {
    try {
      var queryEmbedding = embeddingService.generateEmbedding(taskDescription);
      var pageable = PageRequest.of(0, MAX_RESULTS);
      var results = repository.findTopByEmbeddingSimilarity(queryEmbedding, pageable);

      return results.stream()
          .map(e -> descriptorsByName.get(e.getToolName()))
          .filter(Objects::nonNull)
          .toList();
    } catch (Exception e) {
      log.error("Semantic tool search failed for query: {}", taskDescription, e);
      return List.of();
    }
  }

  @Override
  public Optional<ToolCallback> getCallback(String name) {
    return Optional.ofNullable(callbacksByName.get(name));
  }

  @Override
  public int size() {
    return callbacksByName.size();
  }
}
