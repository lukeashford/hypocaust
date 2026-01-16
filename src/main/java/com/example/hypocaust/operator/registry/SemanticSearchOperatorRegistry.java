package com.example.hypocaust.operator.registry;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.db.OperatorEmbedding;
import com.example.hypocaust.operator.Operator;
import com.example.hypocaust.operator.OperatorSpec;
import com.example.hypocaust.repo.OperatorEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

  private final OperatorEmbeddingRepository repository;
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

    // Reset cache to reflect current discovery
    operatorsByName.clear();

    final var upsertRows = new ArrayList<OperatorEmbedding>();
    final Set<String> currentOperatorNames = new HashSet<>();

    for (final var operator : operators) {
      try {
        final var spec = operator.spec();
        final var operatorName = spec.name();

        // Track discovered operator names
        currentOperatorNames.add(operatorName);

        // Single source of truth - cache only operators
        operatorsByName.put(operatorName, operator);

        // Use ToolSpec's existing description method + context
        final var descriptionText = createEmbeddingText(spec);
        final var existingEmbeddingOpt = repository.findByOperatorName(operatorName);
        final var textHash = hashCalculator.calculateSha256Hash(descriptionText);

        if (existingEmbeddingOpt.isEmpty()) {
          upsertRows.add(OperatorEmbedding.builder()
              .operatorName(operatorName)
              .embedding(embeddingService.generateEmbedding(descriptionText))
              .hash(textHash)
              .build());
        } else if (!Objects.equals(existingEmbeddingOpt.get().getHash(), textHash)) {
          final var existing = existingEmbeddingOpt.get();
          existing.updateEmbedding(embeddingService.generateEmbedding(descriptionText), textHash);
          upsertRows.add(existing);
        }

      } catch (Exception e) {
        log.error("Failed to process operator {}: {}", operator.getClass().getSimpleName(),
            e.getMessage(), e);
      }
    }

    if (!upsertRows.isEmpty()) {
      repository.saveAll(upsertRows);
      log.info("Generated embeddings for {} operators", upsertRows.size());
    }

    // Remove embeddings for operators that no longer exist
    try {
      final var allEmbeddings = repository.findAll();
      final var obsoleteEmbeddings = allEmbeddings.stream()
          .filter(e -> !currentOperatorNames.contains(e.getOperatorName()))
          .toList();
      if (!obsoleteEmbeddings.isEmpty()) {
        repository.deleteAll(obsoleteEmbeddings);
        log.info("Deleted {} obsolete operator embeddings", obsoleteEmbeddings.size());
      }
    } catch (Exception e) {
      log.warn("Failed to clean up obsolete operator embeddings: {}", e.getMessage(), e);
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

      final var results = repository.findTopByEmbeddingSimilarity(queryEmbedding,
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