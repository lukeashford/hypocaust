package com.example.the_machine.rag;

import com.example.the_machine.common.HashCalculator;
import com.example.the_machine.db.WorkflowEmbedding;
import com.example.the_machine.repo.WorkflowEmbeddingRepository;
import com.example.the_machine.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Registry for AI workflow documents. Each document is a single record (no chunking): - First line:
 * '# <Workflow Name>' - Somewhere after: a paragraph starting with 'Summary: ' used for embedding.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowEmbeddingRegistry {

  private static final String SUMMARY_PREFIX = "Summary: ";
  private static final String EXT_MD = ".md";
  private static final String H1_PREFIX = "# ";
  private static final int DEFAULT_MAX_RESULTS = 5;

  private final WorkflowEmbeddingRepository repository;
  private final EmbeddingService embeddingService;
  private final HashCalculator hashCalculator;

  @Value("${app.rag.workflows-path:src/main/resources/rag/workflows}")
  private String workflowsDir;

  @PostConstruct
  public void initialize() {
    try {
      indexDocuments();
    } catch (Exception e) {
      log.error("Failed to index workflow documents", e);
    }
  }

  public void indexDocuments() {
    final var dir = Path.of(workflowsDir);
    if (!Files.exists(dir)) {
      log.warn("Workflows directory not found: {}", workflowsDir);
      return;
    }

    final List<Record> records = new ArrayList<>();

    try (var paths = Files.walk(dir)) {
      paths.filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().toLowerCase().endsWith(EXT_MD))
          .forEach(p -> {
            try {
              final var rec = parseFile(p);
              if (rec != null) {
                records.add(rec);
              }
            } catch (Exception e) {
              log.warn("Failed to parse workflow file {}: {}", p, e.getMessage());
            }
          });
    } catch (IOException e) {
      log.error("Error scanning workflows directory {}", workflowsDir, e);
      return;
    }

    final var upserts = new ArrayList<WorkflowEmbedding>();
    final var currentNames = new HashSet<String>();

    for (final var r : records) {
      currentNames.add(r.name);
      try {
        final var existingOpt = repository.findByName(r.name);
        if (existingOpt.isEmpty()) {
          final var embedding = embeddingService.generateEmbedding(r.embeddingText);
          upserts.add(WorkflowEmbedding.builder()
              .name(r.name)
              .embedding(embedding)
              .hash(r.hash)
              .text(r.fullText)
              .build());
        } else {
          final var existing = existingOpt.get();
          final var requiresReembed = !Objects.equals(existing.getHash(), r.hash);
          final var textChanged = !Objects.equals(existing.getText(), r.fullText);
          if (requiresReembed || textChanged) {
            final float[] newEmbedding = requiresReembed
                ? embeddingService.generateEmbedding(r.embeddingText)
                : null;
            existing.update(r.fullText, r.hash, newEmbedding);
            upserts.add(existing);
          }
        }
      } catch (Exception e) {
        log.warn("Failed to process workflow {}: {}", r.name, e.getMessage());
      }
    }

    if (!upserts.isEmpty()) {
      repository.saveAll(upserts);
      log.info("Generated embeddings for {} workflows", upserts.size());
    }

    // cleanup obsolete
    try {
      final var all = repository.findAll();
      final var obsolete = all.stream()
          .filter(e -> !currentNames.contains(e.getName()))
          .toList();
      if (!obsolete.isEmpty()) {
        repository.deleteAll(obsolete);
        log.info("Deleted {} obsolete workflow embeddings", obsolete.size());
      }
    } catch (Exception e) {
      log.warn("Failed to clean up obsolete workflow embeddings: {}", e.getMessage(), e);
    }
  }

  public List<SearchResult> search(String query) {
    final var pageable = PageRequest.of(0, DEFAULT_MAX_RESULTS);
    try {
      final var queryEmbedding = embeddingService.generateEmbedding(query);
      final var results = repository.findTopByEmbeddingSimilarity(queryEmbedding, pageable);
      final var out = new ArrayList<SearchResult>();
      for (final var r : results) {
        out.add(new SearchResult(r.getName(), r.getText()));
      }
      return out;
    } catch (Exception e) {
      log.error("Workflow semantic search failed for query: {}", query, e);
      return List.of();
    }
  }

  private Record parseFile(Path file) throws IOException {
    final var content = Files.readString(file, StandardCharsets.UTF_8)
        .replace("\r\n", "\n").replace('\r', '\n');

    String name = null;
    String summary = null;

    for (final var line : content.split("\n")) {
      if (name == null && line.startsWith(H1_PREFIX)) {
        name = line.substring(H1_PREFIX.length()).trim();
        continue;
      }
      if (summary == null && line.startsWith(SUMMARY_PREFIX)) {
        summary = line.substring(SUMMARY_PREFIX.length()).strip();
      }
    }

    if (name == null || name.isBlank()) {
      log.warn("Skipping workflow file without H1 heading: {}", file);
      return null;
    }

    if (summary == null || summary.isBlank()) {
      log.warn("No 'Summary:' paragraph found in workflow {}", file);
      return null;
    }

    final var hash = hashCalculator.calculateSha256Hash(summary);
    return new Record(name, content, summary, hash);
  }

  public record SearchResult(String name, String text) {

  }

  private record Record(String name, String fullText, String embeddingText, String hash) {

  }
}
