package com.example.the_machine.rag;

import com.example.the_machine.common.HashCalculator;
import com.example.the_machine.db.PlatformEmbedding;
import com.example.the_machine.repo.PlatformEmbeddingRepository;
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
 * Registry that scans a directory of markdown documents describing AI platforms and their models,
 * generates embeddings per model chunk (based on text following the "Summary:" marker), keeps the
 * database in sync, and provides semantic search over those chunks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformEmbeddingRegistry {

  // Constants
  private static final String SUMMARY_PREFIX = "Summary: ";
  private static final String EXT_MD = ".md";
  private static final String H1_PREFIX = "# ";
  private static final String H2_PREFIX = "## ";
  private static final int DEFAULT_MAX_RESULTS = 5;

  private final PlatformEmbeddingRepository repository;
  private final EmbeddingService embeddingService;
  private final HashCalculator hashCalculator;

  @Value("${app.rag.platforms-path:src/main/resources/rag/platforms}")
  private String platformsDir;

  @PostConstruct
  public void initialize() {
    try {
      indexDocuments();
    } catch (Exception e) {
      log.error("Failed to index platform documents", e);
    }
  }

  /**
   * Executes discovery, change detection, upserts, and deletion of obsolete rows.
   */
  public void indexDocuments() {
    final var dir = Path.of(platformsDir);
    if (!Files.exists(dir)) {
      log.warn("Platforms directory not found: {}", platformsDir);
      return;
    }

    final List<Chunk> chunks = new ArrayList<>();

    try {
      // Collect chunks from all markdown files in directory
      try (var paths = Files.walk(dir)) {
        paths.filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(EXT_MD))
            .forEach(p -> {
              try {
                chunks.addAll(parseFile(p));
              } catch (Exception e) {
                log.warn("Failed to parse file {}: {}", p, e.getMessage());
              }
            });
      }
    } catch (IOException e) {
      log.error("Error scanning directory {}", platformsDir, e);
      return;
    }

    final var upsertRows = new ArrayList<PlatformEmbedding>();
    final var currentNames = new HashSet<String>();

    for (final var ch : chunks) {
      currentNames.add(ch.name);
      try {
        final var existingOpt = repository.findByName(ch.name);
        if (existingOpt.isEmpty()) {
          // New
          final var embedding = embeddingService.generateEmbedding(ch.embeddingText);
          upsertRows.add(PlatformEmbedding.builder()
              .name(ch.name)
              .embedding(embedding)
              .hash(ch.hash)
              .text(ch.fullText)
              .build());
        } else {
          final var existing = existingOpt.get();
          final var requiresReembed = !Objects.equals(existing.getHash(), ch.hash);
          final var textChanged = !Objects.equals(existing.getText(), ch.fullText);
          if (requiresReembed || textChanged) {
            final float[] newEmbedding = requiresReembed
                ? embeddingService.generateEmbedding(ch.embeddingText)
                : null; // keep existing embedding if only text changed
            existing.update(ch.fullText, ch.hash, newEmbedding);
            upsertRows.add(existing);
          }
        }
      } catch (Exception e) {
        log.warn("Failed to process chunk {}: {}", ch.name, e.getMessage());
      }
    }

    if (!upsertRows.isEmpty()) {
      repository.saveAll(upsertRows);
      log.info("Generated embeddings for {} chunks", upsertRows.size());
    }

    // Delete obsolete
    try {
      final var all = repository.findAll();
      final var obsolete = all.stream()
          .filter(e -> !currentNames.contains(e.getName()))
          .toList();
      if (!obsolete.isEmpty()) {
        repository.deleteAll(obsolete);
        log.info("Deleted {} obsolete document embeddings", obsolete.size());
      }
    } catch (Exception e) {
      log.warn("Failed to clean up obsolete document embeddings: {}", e.getMessage(), e);
    }
  }

  /**
   * Performs semantic search over document chunks.
   */
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
      log.error("Document semantic search failed for query: {}", query, e);
      return List.of();
    }
  }

  // Parser
  private List<Chunk> parseFile(Path file) throws IOException {
    final var content = Files.readString(file, StandardCharsets.UTF_8);
    final var lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");

    String platform = null;
    final var models = new ArrayList<ModelSection>();

    String currentModel = null;
    final var buffer = new ArrayList<String>();

    for (final var rawLine : lines) {
      final var line = rawLine; // keep as-is for text
      if (platform == null && line.startsWith(H1_PREFIX)) {
        platform = line.substring(H1_PREFIX.length()).trim();
        continue;
      }
      if (line.startsWith(H2_PREFIX)) {
        // flush previous
        if (currentModel != null) {
          models.add(new ModelSection(currentModel, String.join("\n", buffer)));
          buffer.clear();
        }
        currentModel = line.substring(H2_PREFIX.length()).trim();
        continue;
      }
      if (currentModel != null) {
        buffer.add(line);
      }
    }
    if (currentModel != null) {
      models.add(new ModelSection(currentModel, String.join("\n", buffer)));
    }

    if (platform == null || platform.isBlank()) {
      log.warn("Skipping file without platform H1 heading: {}", file);
      return List.of();
    }

    final var chunks = new ArrayList<Chunk>();

    for (final var m : models) {
      final var fullChunk = buildFullChunk(platform, m.modelName(), m.body());
      final var embeddingText = extractEmbeddingText(m.body());
      if (embeddingText.isEmpty()) {
        log.warn("No '{}' section found for {} - {} in file {}", SUMMARY_PREFIX.trim(), platform,
            m.modelName(), file);
        continue;
      }
      final var hash = hashCalculator.calculateSha256Hash(embeddingText);
      final var name = platform + " - " + m.modelName();
      chunks.add(new Chunk(name, fullChunk, embeddingText, hash));
    }

    return chunks;
  }

  private static String buildFullChunk(String platform, String model, String body) {
    final var sb = new StringBuilder();
    sb.append("# ").append(platform).append('\n');
    sb.append("## ").append(model).append('\n');
    if (body != null && !body.isEmpty()) {
      sb.append(body.strip()).append('\n');
    }
    return sb.toString();
  }

  private static String extractEmbeddingText(String body) {
    if (body == null) {
      return "";
    }
    final var idx = body.indexOf(SUMMARY_PREFIX);
    if (idx < 0) {
      return "";
    }
    final var after = body.substring(idx + SUMMARY_PREFIX.length());
    return after.strip();
  }

  // Data holders
  private record ModelSection(String modelName, String body) {

  }

  public record SearchResult(String name, String text) {

  }

  private record Chunk(String name, String fullText, String embeddingText, String hash) {

  }
}
