package com.example.the_machine.operator.registry;

import com.example.the_machine.common.HashCalculator;
import com.example.the_machine.db.OperatorEmbedding;
import com.example.the_machine.operator.Operator;
import com.example.the_machine.operator.OperatorSpec;
import com.example.the_machine.repo.OperatorEmbeddingRepository;
import com.example.the_machine.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Semantic search-enabled operator registry that uses vector embeddings for intelligent operator
 * discovery and matching. Discovers operators from the Spring context, generates embeddings for
 * their descriptions, and provides semantic search capabilities.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchOperatorRegistry implements OperatorRegistry {

  private final OperatorEmbeddingRepository embeddingRepository;
  private final EmbeddingService embeddingService;
  private final HashCalculator hashCalculator;
  private final List<Operator> operators;

  private final Map<String, Operator> operatorsByName = new ConcurrentHashMap<>();

  @PostConstruct
  public void initialize() {
    discoverAndIndexOperators();
  }

  /**
   * Discovers operators and generates embeddings for new/updated operators.
   */
  private void discoverAndIndexOperators() {
    log.info("Generating operator embeddings...");

    final var newEmbeddings = new ArrayList<OperatorEmbedding>();

    for (final var operator : operators) {
      try {
        final var spec = operator.spec();
        final var operatorName = spec.name();

        // Single source of truth - cache only operators
        operatorsByName.put(operatorName, operator);

        // Use ToolSpec's existing description method + context
        final var descriptionText = createEmbeddingText(spec);
        final var existingEmbedding = embeddingRepository.findByOperatorName(operatorName);
        final var textHash = hashCalculator.calculateSha256Hash(descriptionText);

        if (existingEmbedding.isEmpty() || !existingEmbedding.get().getHash().equals(textHash)) {
          final var embedding = embeddingService.generateEmbedding(descriptionText);

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
  private String createEmbeddingText(OperatorSpec spec) {
    final var text = new StringBuilder();
    text.append("Tool: ").append(spec.name());

    // ToolSpec already has getDescription()!
    if (spec.description() != null && !spec.description().trim().isEmpty()) {
      text.append(" - ").append(spec.description());
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
  public List<OperatorSpec> searchByTask(String taskDescription) {
    final var maxResults = 3;

    try {
      final var queryEmbedding = embeddingService.generateEmbedding(taskDescription);
      final var pageable = PageRequest.of(0, maxResults);

      final var results = embeddingRepository.findTopByEmbeddingSimilarity(queryEmbedding,
          pageable);

      return results.stream()
          .map(embedding -> {
            final var operator = operatorsByName.get(embedding.getOperatorName());
            return operator != null ? operator.spec() : null; // Single source of truth!
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Semantic search failed for query: {}", taskDescription, e);
      return new ArrayList<>();
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
}