package com.example.hypocaust.rag;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.db.ModelEmbedding;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.repo.ModelEmbeddingRepository;
import com.example.hypocaust.service.EmbeddingService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Registry that scans a directory of markdown documents describing AI models, generates embeddings
 * per model chunk (based on human-friendly name and description), keeps the database in sync, and
 * provides semantic search over those chunks.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ModelEmbeddingRegistry {

  // Constants
  private static final String EXT_MD = ".md";
  private static final String H2_PREFIX = "## ";
  private static final int DEFAULT_MAX_RESULTS = 5;

  private final ModelEmbeddingRepository repository;
  private final EmbeddingService embeddingService;
  private final HashCalculator hashCalculator;

  @Value("${app.rag.platforms-path:src/main/resources/rag/platforms}")
  private String platformsDir;

  @PostConstruct
  public void initialize() {
    try {
      indexDocuments();
    } catch (Exception e) {
      log.error("Failed to index model documents", e);
    }
  }

  /**
   * Executes discovery, change detection, upserts, and deletion of obsolete rows.
   */
  public void indexDocuments() {
    final var dir = Path.of(platformsDir);
    if (!Files.exists(dir)) {
      log.warn("Models directory not found: {}", platformsDir);
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

    // 1. Fetch all existing records into a Map to avoid N+1 queries
    final Map<String, ModelEmbedding> existingMap = repository.findAll().stream()
        .collect(Collectors.toMap(ModelEmbedding::getName, e -> e));

    // 2. Process chunks in parallel using virtual threads (via parallelStream)
    final List<ModelEmbedding> upsertRows = chunks.parallelStream()
        .map(ch -> {
          try {
            final ModelEmbedding existing = existingMap.get(ch.name);
            if (existing == null) {
              // New record
              return ModelEmbedding.builder()
                  .name(ch.name)
                  .embedding(embeddingService.generateEmbedding(ch.embeddingText))
                  .hash(ch.hash)
                  .owner(ch.owner)
                  .modelId(ch.modelId)
                  .description(ch.description)
                  .bestPractices(ch.bestPractices)
                  .tier(ch.tier)
                  .platform(ch.platform)
                  .inputs(ch.inputs)
                  .outputs(ch.outputs)
                  .build();
            } else {
              // Check for changes
              final boolean requiresReembed = !Objects.equals(existing.getHash(), ch.hash);
              final boolean metadataChanged = !Objects.equals(existing.getOwner(), ch.owner)
                  || !Objects.equals(existing.getModelId(), ch.modelId)
                  || !Objects.equals(existing.getDescription(), ch.description)
                  || !Objects.equals(existing.getBestPractices(), ch.bestPractices)
                  || !Objects.equals(existing.getTier(), ch.tier)
                  || !Objects.equals(existing.getPlatform(), ch.platform)
                  || !Objects.equals(existing.getInputs(), ch.inputs)
                  || !Objects.equals(existing.getOutputs(), ch.outputs);

              if (requiresReembed || metadataChanged) {
                final float[] newEmbedding = requiresReembed
                    ? embeddingService.generateEmbedding(ch.embeddingText)
                    : null;
                existing.update(ch.hash, newEmbedding, ch.owner, ch.modelId,
                    ch.description, ch.bestPractices, ch.tier, ch.platform,
                    ch.inputs, ch.outputs);
                return existing;
              }
            }
          } catch (Exception e) {
            log.warn("Failed to process chunk {}: {}", ch.name, e.getMessage());
          }
          return null;
        })
        .filter(Objects::nonNull)
        .toList();

    if (!upsertRows.isEmpty()) {
      repository.saveAll(upsertRows);
      log.info("Generated embeddings for {} chunks", upsertRows.size());
    }

    // 3. Efficient cleanup using the map
    final Set<String> currentNames = chunks.stream().map(c -> c.name).collect(Collectors.toSet());
    final List<ModelEmbedding> obsolete = existingMap.values().stream()
        .filter(e -> !currentNames.contains(e.getName()))
        .toList();
    if (!obsolete.isEmpty()) {
      repository.deleteAll(obsolete);
      log.info("Deleted {} obsolete document embeddings", obsolete.size());
    }
  }

  /**
   * Performs semantic search over document chunks filtered by requirements.
   */
  public List<SearchResult> search(ModelRequirement req) {
    final var pageable = PageRequest.of(0, DEFAULT_MAX_RESULTS);
    try {
      final var queryEmbedding = embeddingService.generateEmbedding(req.searchString());

      final var out = new ArrayList<SearchResult>();
      for (final var r :
          repository.findTopByEmbeddingSimilarityFiltered(queryEmbedding, req.tier(), req.output(),
              req.inputs(), pageable)) {
        out.add(new SearchResult(
            r.getName(),
            r.getOwner(),
            r.getModelId(),
            r.getDescription(),
            r.getBestPractices(),
            r.getTier(),
            r.getPlatform(),
            r.getInputs(),
            r.getOutputs()
        ));
      }
      return out;
    } catch (Exception e) {
      log.error("Document semantic search failed for requirements: {}", req, e);
      return List.of();
    }
  }

  // Parser
  List<Chunk> parseFile(Path file) throws IOException {
    final var content = Files.readString(file, StandardCharsets.UTF_8);
    final var lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");

    // Derive platform from filename: replicate.md -> REPLICATE, fal.md -> FAL, openrouter.md -> OPENROUTER
    final var platform = derivePlatform(file.getFileName().toString());

    final var models = new ArrayList<ModelSection>();

    String currentModelTitle = null;
    final var buffer = new ArrayList<String>();

    for (final var line : lines) {
      if (line.startsWith(H2_PREFIX)) {
        // flush previous
        if (currentModelTitle != null) {
          models.add(new ModelSection(currentModelTitle, String.join("\n", buffer)));
          buffer.clear();
        }
        currentModelTitle = line.substring(H2_PREFIX.length()).trim();
        continue;
      }
      if (currentModelTitle != null) {
        buffer.add(line);
      }
    }
    if (currentModelTitle != null) {
      models.add(new ModelSection(currentModelTitle, String.join("\n", buffer)));
    }

    final var chunks = new ArrayList<Chunk>();

    for (final var m : models) {
      try {
        chunks.add(parseModelChunk(m, platform));
      } catch (Exception e) {
        log.warn("Failed to parse model chunk '{}' in file {}: {}", m.modelName(), file,
            e.getMessage());
      }
    }

    return chunks;
  }

  static String derivePlatform(String filename) {
    var name = filename.toLowerCase();
    if (name.endsWith(EXT_MD)) {
      name = name.substring(0, name.length() - EXT_MD.length());
    }
    return name.toUpperCase();
  }

  private Chunk parseModelChunk(ModelSection m, String platform) {
    String owner = "";
    String modelId = "";
    String tier = "balanced";
    String description = "";
    String bestPractices = "";
    Set<ArtifactKind> inputs = new HashSet<>();
    Set<ArtifactKind> outputs = new HashSet<>();

    String[] lines = m.body().split("\n");
    int i = 0;
    while (i < lines.length) {
      String line = lines[i].trim();
      if (line.startsWith("- **owner**:")) {
        owner = line.substring("- **owner**:".length()).trim();
      } else if (line.startsWith("- **id**:")) {
        modelId = line.substring("- **id**:".length()).trim();
      } else if (line.startsWith("- **tier**:")) {
        tier = line.substring("- **tier**:".length()).trim();
      } else if (line.startsWith("- **input**:")) {
        String val = line.substring("- **input**:".length()).trim();
        Arrays.stream(val.split(",")).map(String::trim).map(String::toUpperCase)
            .map(ArtifactKind::valueOf).forEach(inputs::add);
      } else if (line.startsWith("- **output**:")) {
        String val = line.substring("- **output**:".length()).trim();
        Arrays.stream(val.split(",")).map(String::trim).map(String::toUpperCase)
            .map(ArtifactKind::valueOf).forEach(outputs::add);
      } else if (line.startsWith("- **ID**:")) {
        String id = line.substring("- **ID**:".length()).trim();
        String[] parts = id.split("/");
        if (parts.length == 2) {
          owner = parts[0].trim();
          modelId = parts[1].trim();
        } else {
          modelId = id;
        }
      } else if (line.equals("### Description")) {
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < lines.length && !lines[i].startsWith("###")) {
          sb.append(lines[i]).append("\n");
          i++;
        }
        description = sb.toString().trim();
        continue;
      } else if (line.equals("### Best Practices")) {
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < lines.length && !lines[i].startsWith("###")) {
          sb.append(lines[i]).append("\n");
          i++;
        }
        bestPractices = sb.toString().trim();
        continue;
      }
      i++;
    }

    if (modelId.isEmpty()) {
      throw new IllegalArgumentException("Missing required metadata (ID)");
    }

    if (inputs.isEmpty() || outputs.isEmpty()) {
      throw new IllegalArgumentException(
          "Model " + m.modelName()
              + " must have at least one input and output ArtifactKind defined.");
    }

    String hash = hashCalculator.calculateSha256Hash(
        description + " tier: " + tier + " inputs: " + inputs + " outputs: " + outputs);

    String embeddingText =
        m.modelName() + " " + description + " tier: " + tier + " inputs: " + inputs + " outputs: "
            + outputs;

    return new Chunk(m.modelName(), embeddingText, hash, owner, modelId, description,
        bestPractices, tier, platform, inputs, outputs);
  }

  // Data holders
  private record ModelSection(String modelName, String body) {

  }

  public record SearchResult(
      String name,
      String owner,
      String modelId,
      String description,
      String bestPractices,
      String tier,
      String platform,
      Set<ArtifactKind> inputs,
      Set<ArtifactKind> outputs
  ) {

  }

  record Chunk(
      String name,
      String embeddingText,
      String hash,
      String owner,
      String modelId,
      String description,
      String bestPractices,
      String tier,
      String platform,
      Set<ArtifactKind> inputs,
      Set<ArtifactKind> outputs
  ) {

  }
}
