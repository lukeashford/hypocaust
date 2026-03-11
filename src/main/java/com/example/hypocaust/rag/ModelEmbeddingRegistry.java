package com.example.hypocaust.rag;

import com.example.hypocaust.common.HashCalculator;
import com.example.hypocaust.common.JsonUtils;
import com.example.hypocaust.db.ModelEmbedding;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.OutputSpec;
import com.example.hypocaust.models.enums.AnthropicChatModelSpec;
import com.example.hypocaust.prompt.fragments.PromptFragments;
import com.example.hypocaust.repo.ModelEmbeddingRepository;
import com.example.hypocaust.service.ChatService;
import com.example.hypocaust.service.EmbeddingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.jackson.Jacksonized;
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
  private static final String EXT_JSON = ".json";
  private static final int DEFAULT_MAX_RESULTS = 10;
  /**
   * Candidate pool fetched from DB before soft-ranking.
   */
  private static final int CANDIDATE_POOL_SIZE = 15;
  /**
   * Pool passed to the LLM reranker after soft-ranking.
   */
  private static final int RERANK_POOL_SIZE = 12;

  // Tier soft-ranking weights
  private static final Map<String, Integer> TIER_ORDINALS = Map.of(
      "fast", 0, "balanced", 1, "powerful", 2);
  /**
   * Penalty (in rank positions) per tier step upward (more expensive than needed).
   */
  private static final double UP_TIER_PENALTY = 3.0;
  /**
   * Penalty (in rank positions) per tier step downward (cheaper/faster than requested).
   */
  private static final double DOWN_TIER_PENALTY = 1.0;

  private final ModelEmbeddingRepository repository;
  private final EmbeddingService embeddingService;
  private final ChatService chatService;
  private final HashCalculator hashCalculator;
  private final ObjectMapper objectMapper;

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
      // Collect chunks from all JSON files in directory
      try (var paths = Files.walk(dir)) {
        paths.filter(Files::isRegularFile)
            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(EXT_JSON))
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
              log.info("Generating embedding for chunk {}", ch.name);
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
                  .optionalInputs(ch.optionalInputs)
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
                  || !Objects.equals(existing.getOptionalInputs(), ch.optionalInputs)
                  || !Objects.equals(existing.getOutputs(), ch.outputs);

              if (requiresReembed || metadataChanged) {
                log.info("Updating embedding for chunk {}", ch.name);
                final float[] newEmbedding = requiresReembed
                    ? embeddingService.generateEmbedding(ch.embeddingText)
                    : null;
                existing.update(ch.hash, newEmbedding, ch.owner, ch.modelId,
                    ch.description, ch.bestPractices, ch.tier, ch.platform,
                    ch.inputs, ch.optionalInputs, ch.outputs);
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
   *
   * <p>Pipeline:
   * <ol>
   *   <li>DB: fetch {@value CANDIDATE_POOL_SIZE} candidates by cosine distance, filtered only by
   *       required input types (tier is intentionally NOT a hard filter here).
   *   <li>Java: apply asymmetric tier penalty and re-sort; choosing a more expensive/slower tier
   *       than the task requires is penalised more heavily than choosing a cheaper one.
   *   <li>LLM: rerank the top {@value RERANK_POOL_SIZE} candidates using Haiku for final
   *       semantic judgement.
   *   <li>Return the top {@value DEFAULT_MAX_RESULTS} results.
   * </ol>
   */
  public List<ModelSearchResult> search(ModelRequirement req) {
    try {
      final var queryEmbedding = embeddingService.generateEmbedding(req.searchString());

      // Step 1: fetch larger candidate pool — tier is a soft signal, not a hard filter
      final var dbResults = repository.findTopByInputsAndSimilarity(
          queryEmbedding,
          req.inputs(),
          req.outputs() != null ? req.outputs() : Set.of(),
          PageRequest.of(0, CANDIDATE_POOL_SIZE));
      if (dbResults.isEmpty()) {
        return List.of();
      }

      // Step 2: soft-rank by tier penalty
      final var softRanked = softRankByTier(dbResults, req.tier());

      // Step 3: LLM reranking on the top RERANK_POOL_SIZE candidates
      final var rerankPool = softRanked.stream().limit(RERANK_POOL_SIZE).toList();
      final var reranked = llmRerank(rerankPool, req);

      // Step 4: return top DEFAULT_MAX_RESULTS
      return reranked.stream()
          .limit(DEFAULT_MAX_RESULTS)
          .map(this::toSearchResult)
          .toList();

    } catch (Exception e) {
      log.error("Document semantic search failed for requirements: {}", req, e);
      return List.of();
    }
  }

  /**
   * Re-sorts candidates by combining their cosine-rank position with an asymmetric tier penalty.
   * Choosing upward (more expensive/slower) is penalised {@value UP_TIER_PENALTY}× more than
   * choosing downward (cheaper/faster).
   */
  private List<ModelEmbedding> softRankByTier(List<ModelEmbedding> results, String requestedTier) {
    final int reqOrd = TIER_ORDINALS.getOrDefault(requestedTier, 1);
    record Scored(ModelEmbedding model, double score) {

    }

    return IntStream.range(0, results.size())
        .mapToObj(i -> {
          final int actOrd = TIER_ORDINALS.getOrDefault(results.get(i).getTier(), 1);
          final int diff = actOrd - reqOrd;
          final double penalty = diff == 0 ? 0.0
              : diff > 0 ? diff * UP_TIER_PENALTY
                  : Math.abs(diff) * DOWN_TIER_PENALTY;
          return new Scored(results.get(i), i + penalty);
        })
        .sorted(Comparator.comparingDouble(Scored::score))
        .map(Scored::model)
        .toList();
  }

  /**
   * Calls the LLM to rerank the candidate list. Falls back to the input order on any error.
   */
  private List<ModelEmbedding> llmRerank(List<ModelEmbedding> candidates, ModelRequirement req) {
    if (candidates.size() <= 1) {
      return candidates;
    }
    try {
      final var sb = new StringBuilder();
      sb.append("Task: ").append(req.searchString()).append('\n');
      sb.append("Desired tier: ").append(req.tier()).append("\n\n");
      sb.append("Candidate models:\n");
      for (int i = 0; i < candidates.size(); i++) {
        final var m = candidates.get(i);
        sb.append(i + 1).append(". ").append(m.getName())
            .append(" (tier: ").append(m.getTier()).append(")\n");
        sb.append("   ").append(m.getDescription()).append('\n');
      }

      final var response = chatService.call(
          AnthropicChatModelSpec.CLAUDE_HAIKU_4_5,
          PromptFragments.modelReranking().text(),
          sb.toString());

      return parseRerankResponse(response, candidates);
    } catch (Exception e) {
      log.warn("LLM reranking failed, using soft-ranked order: {}", e.getMessage());
      return candidates;
    }
  }

  private List<ModelEmbedding> parseRerankResponse(String response,
      List<ModelEmbedding> candidates) {
    try {
      final var json = JsonUtils.extractJson(response);
      final List<String> rankedNames = objectMapper.readValue(json,
          new TypeReference<>() {
          });

      final var byName = candidates.stream()
          .collect(Collectors.toMap(ModelEmbedding::getName, e -> e));

      final var reranked = new ArrayList<ModelEmbedding>();
      for (final var name : rankedNames) {
        final var m = byName.remove(name);
        if (m != null) {
          reranked.add(m);
        }
      }
      // Append any candidates the LLM omitted (safety net)
      reranked.addAll(byName.values());
      return reranked;
    } catch (Exception e) {
      log.warn("Failed to parse LLM reranking response, using original order: {}", e.getMessage());
      return candidates;
    }
  }

  private ModelSearchResult toSearchResult(ModelEmbedding r) {
    return new ModelSearchResult(
        r.getName(), r.getOwner(), r.getModelId(),
        r.getDescription(), r.getBestPractices(),
        r.getTier(), r.getPlatform(),
        r.getInputs(), r.getOptionalInputs(), r.getOutputs());
  }

  // Parser
  List<Chunk> parseFile(Path file) throws IOException {
    final var platform = derivePlatform(file.getFileName().toString());
    final List<ModelJson> models = objectMapper.readValue(file.toFile(),
        new TypeReference<>() {
        });

    final var chunks = new ArrayList<Chunk>();
    for (final var m : models) {
      try {
        chunks.add(toChunk(m, platform));
      } catch (Exception e) {
        log.warn("Failed to process model '{}' in file {}: {}", m.name(), file, e.getMessage());
      }
    }
    return chunks;
  }

  static String derivePlatform(String filename) {
    var name = filename.toLowerCase();
    if (name.endsWith(EXT_JSON)) {
      name = name.substring(0, name.length() - EXT_JSON.length());
    }
    return name.toUpperCase();
  }

  private String toStableInputsString(Set<ArtifactKind> set) {
    return set.stream()
        .sorted()
        .toList()
        .toString();
  }

  private String toStableOutputsString(Set<OutputSpec> set) {
    return set.stream()
        .map(o -> o.getKind().name() + ":" + o.getDescription())
        .sorted()
        .toList()
        .toString();
  }

  private Chunk toChunk(ModelJson m, String platform) {
    if (m.id() == null || m.id().isEmpty()) {
      throw new IllegalArgumentException("Missing required metadata (id)");
    }
    if (m.inputs() == null || m.inputs().isEmpty() || m.outputs() == null || m.outputs()
        .isEmpty()) {
      throw new IllegalArgumentException(
          "Model " + m.name() + " must have at least one input and output defined.");
    }

    Set<ArtifactKind> optInputs = m.optionalInputs() != null ? m.optionalInputs() : Set.of();

    String stableInputs = toStableInputsString(m.inputs());
    String stableOutputs = toStableOutputsString(m.outputs());

    String embeddingText =
        m.name() + " " + m.description() + " tier: " + m.tier() + " inputs: " + stableInputs
            + " outputs: " + stableOutputs;
    if (!optInputs.isEmpty()) {
      embeddingText += " optionalInputs: " + toStableInputsString(optInputs);
    }

    String hash = hashCalculator.calculateSha256Hash(embeddingText);

    return new Chunk(m.name(), embeddingText, hash, m.owner(), m.id(), m.description(),
        m.bestPractices(), m.tier(), platform, m.inputs(), optInputs, m.outputs());
  }

  // Data holders
  @Builder
  @Jacksonized
  private record ModelJson(
      String name,
      String owner,
      String id,
      String tier,
      Set<ArtifactKind> inputs,
      Set<ArtifactKind> optionalInputs,
      Set<OutputSpec> outputs,
      String description,
      String bestPractices
  ) {

  }

  public record ModelSearchResult(
      String name,
      String owner,
      String modelId,
      String description,
      String bestPractices,
      String tier,
      String platform,
      Set<ArtifactKind> inputs,
      Set<ArtifactKind> optionalInputs,
      Set<OutputSpec> outputs
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
      Set<ArtifactKind> optionalInputs,
      Set<OutputSpec> outputs
  ) {

  }
}
