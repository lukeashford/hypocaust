package com.example.the_machine.operator.registry;

import com.example.the_machine.domain.OperatorEmbedding;
import com.example.the_machine.operator.Operator;
import com.example.the_machine.operator.ToolSpec;
import com.example.the_machine.repo.OperatorEmbeddingRepository;
import com.example.the_machine.service.EmbeddingService;
import com.example.the_machine.service.HashCalculationService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Semantic search-enabled operator registry that uses vector embeddings for intelligent operator
 * discovery and matching. Discovers operators via ServiceLoader, generates embeddings for their
 * descriptions, and provides semantic search capabilities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchOperatorRegistry implements OperatorRegistry {

  private final OperatorEmbeddingRepository embeddingRepository;
  private final EmbeddingService embeddingService;
  private final HashCalculationService hashCalculationService;

  private final Map<String, Operator> operatorsByName = new ConcurrentHashMap<>();

  @PostConstruct
  public void initialize() {
    discoverAndIndexOperators();
  }

  /**
   * Discovers operators using ServiceLoader and generates embeddings for new operators.
   */
  private void discoverAndIndexOperators() {
    log.info("Discovering operators and generating embeddings...");

    val serviceLoader = ServiceLoader.load(Operator.class);
    val newEmbeddings = new ArrayList<OperatorEmbedding>();

    for (val operator : serviceLoader) {
      try {
        val spec = operator.spec();
        val operatorName = spec.getName();

        // Single source of truth - cache only operators
        operatorsByName.put(operatorName, operator);

        // Use ToolSpec's existing description method + context
        val descriptionText = createEmbeddingText(spec);
        val existingEmbedding = embeddingRepository.findByOperatorName(operatorName);
        val textHash = hashCalculationService.calculateSha256Hash(descriptionText);

        if (existingEmbedding.isEmpty() || !existingEmbedding.get().getHash().equals(textHash)) {
          val embedding = embeddingService.generateEmbedding(descriptionText);

          newEmbeddings.add(OperatorEmbedding.builder()
              .operatorName(operatorName)
              .embedding(embedding)
              .hash(textHash)
              .build());
        }

      } catch (Exception e) {
        log.error("Failed to process operator {}: {}", operator.getClass().getSimpleName(),
            e.getMessage(), e);
      }
    }

    if (!newEmbeddings.isEmpty()) {
      embeddingRepository.saveAll(newEmbeddings);
      log.info("Generated embeddings for {} operators", newEmbeddings.size());
    }

    log.info("Registry initialization complete. Indexed {} operators", operatorsByName.size());
  }

  /**
   * Creates embedding text from ToolSpec using existing methods.
   */
  private String createEmbeddingText(ToolSpec spec) {
    val text = new StringBuilder();
    text.append("Tool: ").append(spec.getName());

    // ToolSpec already has getDescription()!
    if (spec.getDescription() != null && !spec.getDescription().trim().isEmpty()) {
      text.append(" - ").append(spec.getDescription());
    }

    // Add I/O context for better matching
    if (!spec.getInputKeys().isEmpty()) {
      text.append(" | Inputs: ").append(String.join(", ", spec.getInputKeys()));
    }

    if (!spec.getOutputKeys().isEmpty()) {
      text.append(" | Outputs: ").append(String.join(", ", spec.getOutputKeys()));
    }

    return text.toString();
  }

  /**
   * Performs semantic search for operators based on task description.
   *
   * @param taskDescription the task description to search for
   * @return list of tool specs ordered by similarity
   */
  @Override
  public List<ToolSpec> searchByTask(String taskDescription) {
    val maxResults = 3;

    try {
      val queryEmbedding = embeddingService.generateEmbedding(taskDescription);
      val pageable = PageRequest.of(0, maxResults);

      val results = embeddingRepository.findTopByEmbeddingSimilarity(queryEmbedding, pageable);

      return results.stream()
          .map(embedding -> {
            val operator = operatorsByName.get(embedding.getOperatorName());
            return operator != null ? operator.spec() : null; // Single source of truth!
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Semantic search failed for query: {}", taskDescription, e);
      return List.of();
    }
  }

  @Override
  public Optional<Operator> get(String name) {
    return Optional.ofNullable(operatorsByName.get(name));
  }

  @Override
  public int size() {
    return operatorsByName.size();
  }

  // Package-private methods for testing
  Map<String, Operator> getOperatorsByName() {
    return operatorsByName;
  }

  String createEmbeddingTextForTesting(ToolSpec spec) {
    return createEmbeddingText(spec);
  }
}