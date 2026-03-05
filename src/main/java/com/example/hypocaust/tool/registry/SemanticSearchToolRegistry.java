package com.example.hypocaust.tool.registry;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.db.ToolEmbedding;
import com.example.hypocaust.repo.ToolEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
@Slf4j
@RequiredArgsConstructor
public class SemanticSearchToolRegistry implements ToolRegistry, SmartInitializingSingleton {

  private static final int MAX_RESULTS = 3;

  private final ToolEmbeddingRepository repository;
  private final EmbeddingService embeddingService;
  private final HashCalculator hashCalculator;
  private final ApplicationContext applicationContext;

  private final Map<String, ToolCallback> discoverableCallbacks = new ConcurrentHashMap<>();

  @Override
  public void afterSingletonsInstantiated() {
    indexDiscoverableTools();
  }

  private void indexDiscoverableTools() {
    log.info("Discovering and indexing tools...");

    // 1. Discover methods with both @DiscoverableTool and @Tool
    List<ToolCallback> tools = findDiscoverableTools();
    tools.forEach(cb -> discoverableCallbacks.put(cb.getToolDefinition().name(), cb));

    // 2. Sync with vector database
    final Map<String, ToolEmbedding> existingMap = repository.findAll().stream()
        .collect(Collectors.toMap(ToolEmbedding::getToolName, e -> e));

    final List<ToolEmbedding> upsertRows = tools.stream()
        .map(cb -> createOrUpdateEmbedding(cb, existingMap))
        .filter(Objects::nonNull)
        .toList();

    if (!upsertRows.isEmpty()) {
      repository.saveAll(upsertRows);
      log.info("Generated embeddings for {} tools", upsertRows.size());
    }

    cleanUpObsoleteEmbeddings(tools, existingMap);
    log.info("Registry initialized with {} tools", discoverableCallbacks.size());
  }

  private List<ToolCallback> findDiscoverableTools() {
    return Arrays.stream(applicationContext.getBeanNamesForType(Object.class))
        .flatMap(name -> {
          try {
            Object bean = applicationContext.getBean(name);
            Class<?> userClass = ClassUtils.getUserClass(bean);
            return MethodIntrospector.selectMethods(userClass,
                    (MethodIntrospector.MetadataLookup<DiscoverableTool>) method ->
                        AnnotatedElementUtils.findMergedAnnotation(method, DiscoverableTool.class))
                .keySet().stream()
                .filter(m -> AnnotatedElementUtils.findMergedAnnotation(m, Tool.class) != null)
                .map(m -> (ToolCallback) MethodToolCallback.builder()
                    .toolDefinition(ToolDefinitions.from(m))
                    .toolMethod(m)
                    .toolObject(bean)
                    .build());
          } catch (Exception e) {
            return Stream.empty();
          }
        })
        .toList();
  }

  private ToolEmbedding createOrUpdateEmbedding(ToolCallback cb,
      Map<String, ToolEmbedding> existingMap) {
    ToolDefinition def = cb.getToolDefinition();
    String text =
        "Tool: " + def.name() + " - " + def.description() + " | Schema: " + def.inputSchema();
    String hash = hashCalculator.calculateSha256Hash(text);
    ToolEmbedding existing = existingMap.get(def.name());

    if (existing == null) {
      return ToolEmbedding.builder()
          .toolName(def.name())
          .embedding(embeddingService.generateEmbedding(text))
          .hash(hash)
          .build();
    } else if (!Objects.equals(existing.getHash(), hash)) {
      existing.updateEmbedding(embeddingService.generateEmbedding(text), hash);
      return existing;
    }
    return null;
  }

  private void cleanUpObsoleteEmbeddings(List<ToolCallback> current,
      Map<String, ToolEmbedding> existing) {
    var currentNames = current.stream().map(cb -> cb.getToolDefinition().name())
        .collect(Collectors.toSet());
    var obsolete = existing.values().stream().filter(e -> !currentNames.contains(e.getToolName()))
        .toList();
    if (!obsolete.isEmpty()) {
      repository.deleteAll(obsolete);
      log.info("Deleted {} obsolete tool embeddings", obsolete.size());
    }
  }

  @Override
  public List<ToolDefinition> searchByTask(String taskDescription) {
    try {
      var queryEmbedding = embeddingService.generateEmbedding(taskDescription);
      return repository
          .findTopByEmbeddingSimilarity(queryEmbedding, PageRequest.of(0, MAX_RESULTS))
          .stream()
          .map(e -> discoverableCallbacks.get(e.getToolName()))
          .filter(Objects::nonNull)
          .map(ToolCallback::getToolDefinition)
          .toList();
    } catch (Exception e) {
      log.error("Semantic search failed for: {}", taskDescription, e);
      return List.of();
    }
  }

  @Override
  public Optional<ToolCallback> getCallback(String name) {
    return Optional.ofNullable(discoverableCallbacks.get(name));
  }

  @Override
  public int size() {
    return discoverableCallbacks.size();
  }
}