package com.example.hypocaust.tool.registry;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.db.ToolEmbedding;
import com.example.hypocaust.repo.ToolEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Semantic search-enabled tool registry. Discovers all {@link DiscoverableTool}-annotated beans,
 * generates embeddings for their descriptions, and provides semantic search.
 *
 * <p>Preserves the same elegant auto-registration pattern as the former
 * {@code SemanticSearchOperatorRegistry}: just write a new tool class with the annotation and
 * Spring picks it up.
 */
@Component
@Slf4j
public class SemanticSearchToolRegistry implements ToolRegistry, SmartInitializingSingleton {

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

  @Override
  public void afterSingletonsInstantiated() {
    discoverAndIndexTools();
  }

  private void discoverAndIndexTools() {
    log.info("Discovering and indexing tools...");

    final List<ToolCandidate> candidates = new ArrayList<>();
    final String[] beanNames = applicationContext.getBeanDefinitionNames();

    for (String beanName : beanNames) {
      Class<?> beanType = applicationContext.getType(beanName);
      if (beanType == null) {
        continue;
      }

      boolean isCandidate = AnnotatedElementUtils.hasAnnotation(beanType, DiscoverableTool.class);
      if (!isCandidate) {
        for (Method method : beanType.getMethods()) {
          if (AnnotatedElementUtils.hasAnnotation(method, DiscoverableTool.class)) {
            isCandidate = true;
            break;
          }
        }
      }

      if (!isCandidate) {
        continue;
      }

      Object bean = applicationContext.getBean(beanName);
      Class<?> beanClass = bean.getClass();

      // 1. Look for methods explicitly marked as discoverable
      boolean foundMethodTool = false;
      for (Method method : beanClass.getMethods()) {
        var disc = AnnotatedElementUtils.findMergedAnnotation(method, DiscoverableTool.class);
        if (disc != null) {
          candidates.add(new ToolCandidate(bean, method, disc.name(), disc.description()));
          foundMethodTool = true;
        }
      }

      // 2. Fallback: If class is marked but no method is, find the "primary" method
      if (!foundMethodTool && AnnotatedElementUtils.hasAnnotation(beanClass,
          DiscoverableTool.class)) {
        var classDisc = AnnotatedElementUtils.findMergedAnnotation(beanClass,
            DiscoverableTool.class);
        Method primary = findPrimaryMethod(beanClass);
        if (primary != null) {
          candidates.add(
              new ToolCandidate(bean, primary, classDisc.name(), classDisc.description()));
        } else {
          log.warn("Class {} is marked @DiscoverableTool but has no unique primary method",
              beanClass.getSimpleName());
        }
      }
    }

    // 3. Pre-fetch existing embeddings to avoid N+1
    final Map<String, ToolEmbedding> existingMap = repository.findAll().stream()
        .collect(Collectors.toMap(ToolEmbedding::getToolName, e -> e));

    // 4. Parallel process each candidate
    final List<ToolEmbedding> upsertRows = candidates.parallelStream()
        .map(c -> registerAndIndex(c, existingMap))
        .filter(Objects::nonNull)
        .toList();

    if (!upsertRows.isEmpty()) {
      repository.saveAll(upsertRows);
      log.info("Generated embeddings for {} tools", upsertRows.size());
    }

    // Clean up obsolete embeddings
    final Set<String> currentToolNames = candidates.stream()
        .map(c -> resolveToolName(c.method(), c.name()))
        .collect(Collectors.toSet());

    try {
      var obsolete = existingMap.values().stream()
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

  private ToolEmbedding registerAndIndex(ToolCandidate c, Map<String, ToolEmbedding> existingMap) {
    final Object bean = c.bean();
    final Method toolMethod = c.method();
    final String name = c.name();
    final String description = c.description();

    // Determine metadata
    final String toolName = resolveToolName(toolMethod, name);
    final String toolDescription = resolveToolDescription(toolMethod, description);

    // Build ToolCallback using Spring AI's infrastructure
    final var callback = MethodToolCallback.builder()
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
    final var descriptor = new ToolDescriptor(
        toolName,
        toolDescription,
        buildParameterSchema(toolMethod)
    );
    descriptorsByName.put(toolName, descriptor);

    // Generate embedding
    try {
      final var embeddingText = createEmbeddingText(toolName, toolDescription, toolMethod);
      final var textHash = hashCalculator.calculateSha256Hash(embeddingText);
      final ToolEmbedding existing = existingMap.get(toolName);

      if (existing == null) {
        return ToolEmbedding.builder()
            .toolName(toolName)
            .embedding(embeddingService.generateEmbedding(embeddingText))
            .hash(textHash)
            .build();
      } else if (!Objects.equals(existing.getHash(), textHash)) {
        existing.updateEmbedding(embeddingService.generateEmbedding(embeddingText), textHash);
        return existing;
      }
    } catch (Exception e) {
      log.error("Failed to process tool {}: {}", toolName, e.getMessage(), e);
    }
    return null;
  }

  private String resolveToolName(Method toolMethod, String name) {
    var toolAnnotation = AnnotatedElementUtils.findMergedAnnotation(toolMethod, Tool.class);
    return (name != null && !name.isEmpty()) ? name :
        (toolAnnotation != null && !toolAnnotation.name().isEmpty()) ? toolAnnotation.name()
            : toolMethod.getName();
  }

  private String resolveToolDescription(Method toolMethod, String description) {
    var toolAnnotation = AnnotatedElementUtils.findMergedAnnotation(toolMethod, Tool.class);
    return (description != null && !description.isEmpty()) ? description :
        (toolAnnotation != null ? toolAnnotation.description() : "");
  }

  private record ToolCandidate(Object bean, Method method, String name, String description) {

  }

  private Method findPrimaryMethod(Class<?> clazz) {
    // Logic: find the only public method that isn't from Object or synthetic
    var methods = Arrays.stream(clazz.getMethods())
        .filter(m -> m.getDeclaringClass() != Object.class)
        .filter(m -> !m.isSynthetic())
        .toList();
    return methods.size() == 1 ? methods.get(0) : null;
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
      if (param.getType().isEnum()) {
        var enumConstants = param.getType().getEnumConstants();
        sb.append(",\"enum\":[");
        for (int j = 0; j < enumConstants.length; j++) {
          if (j > 0) {
            sb.append(",");
          }
          sb.append("\"").append(enumConstants[j]).append("\"");
        }
        sb.append("]");
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
