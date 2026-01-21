package com.example.hypocaust.service;

import com.example.hypocaust.db.ArtifactEntity;
import com.example.hypocaust.db.ArtifactRelationEntity;
import com.example.hypocaust.domain.ArtifactGraph;
import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactNode;
import com.example.hypocaust.domain.ArtifactRelation;
import com.example.hypocaust.domain.Provenance;
import com.example.hypocaust.domain.RelationType;
import com.example.hypocaust.domain.SemanticAnchor;
import com.example.hypocaust.exception.AnchorNotFoundException;
import com.example.hypocaust.repo.AnchorEmbeddingRepository;
import com.example.hypocaust.repo.ArtifactRelationRepository;
import com.example.hypocaust.repo.ArtifactRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing the artifact graph and semantic anchor-based queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArtifactGraphService {

  private final ArtifactRepository artifactRepository;
  private final ArtifactRelationRepository relationRepository;
  private final AnchorEmbeddingRepository embeddingRepository;
  private final EmbeddingService embeddingService;
  private final ObjectMapper objectMapper;

  /**
   * Build the artifact graph for a project.
   * This creates a snapshot of all current artifacts and their relations.
   */
  public ArtifactGraph buildGraph(UUID projectId) {
    log.debug("Building artifact graph for project: {}", projectId);

    var builder = new ArtifactGraph.Builder();

    // Load all artifacts for the project
    var artifacts = artifactRepository.findByProjectIdOrderByCreatedAtDesc(projectId);

    for (var entity : artifacts) {
      var node = toArtifactNode(entity);
      if (node != null) {
        builder.addNode(node);
      }
    }

    // Load all relations
    for (var entity : artifacts) {
      var relations = relationRepository.findBySourceArtifactId(entity.getId());
      for (var rel : relations) {
        builder.addRelation(new ArtifactRelation(
            rel.getSourceArtifactId(),
            rel.getTargetArtifactId(),
            rel.getRelationType()
        ));
      }
    }

    var graph = builder.build();
    log.debug("Built graph with {} nodes and {} current artifacts",
        graph.totalNodeCount(), graph.currentArtifactCount());

    return graph;
  }

  /**
   * Build the artifact graph for a specific branch.
   */
  public ArtifactGraph buildGraphForBranch(UUID branchId) {
    log.debug("Building artifact graph for branch: {}", branchId);

    var builder = new ArtifactGraph.Builder();

    // Load artifacts on this branch
    var artifacts = artifactRepository.findByBranchIdOrderByCreatedAtDesc(branchId);

    for (var entity : artifacts) {
      var node = toArtifactNode(entity);
      if (node != null) {
        builder.addNode(node);
      }
    }

    // Load relations
    for (var entity : artifacts) {
      var relations = relationRepository.findBySourceArtifactId(entity.getId());
      for (var rel : relations) {
        builder.addRelation(new ArtifactRelation(
            rel.getSourceArtifactId(),
            rel.getTargetArtifactId(),
            rel.getRelationType()
        ));
      }
    }

    return builder.build();
  }

  /**
   * Find artifacts by semantic description using full-text search.
   */
  public List<ArtifactNode> findByDescription(UUID projectId, String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    log.debug("Searching artifacts by description in project {}: '{}'", projectId, query);

    var entities = artifactRepository.searchByAnchorDescription(projectId, query);
    return entities.stream()
        .map(this::toArtifactNode)
        .filter(node -> node != null)
        .toList();
  }

  /**
   * Find artifacts by semantic similarity using embeddings.
   */
  public List<ArtifactNode> findSimilar(UUID projectId, String query, int limit) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    log.debug("Finding similar artifacts in project {}: '{}'", projectId, query);

    // Generate embedding for the query
    var embedding = embeddingService.generateEmbedding(query);
    if (embedding == null) {
      log.warn("Failed to generate embedding for query: {}", query);
      return findByDescription(projectId, query); // Fall back to text search
    }

    // Search by embedding
    var artifactIds = embeddingRepository.findSimilarArtifactsInProject(embedding, projectId, limit);

    return artifactIds.stream()
        .map(artifactRepository::findById)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::toArtifactNode)
        .filter(node -> node != null)
        .toList();
  }

  /**
   * Find artifact by exact anchor description.
   */
  public Optional<ArtifactNode> findByExactDescription(UUID projectId, String description) {
    return artifactRepository.findCurrentByAnchorDescription(projectId, description)
        .map(this::toArtifactNode);
  }

  /**
   * Find artifact by role.
   */
  public Optional<ArtifactNode> findByRole(UUID projectId, String role) {
    return artifactRepository.findByProjectIdAndAnchorRoleAndSupersededByIdIsNull(projectId, role)
        .map(this::toArtifactNode);
  }

  /**
   * Resolve an @anchor: reference to an artifact.
   * Throws if no matching artifact is found.
   */
  public ArtifactNode resolveAnchor(UUID projectId, String anchorQuery) {
    // First try exact match
    var exact = findByExactDescription(projectId, anchorQuery);
    if (exact.isPresent()) {
      return exact.get();
    }

    // Try semantic search
    var similar = findByDescription(projectId, anchorQuery);
    if (!similar.isEmpty()) {
      return similar.get(0);
    }

    // Try embedding-based search
    var embedded = findSimilar(projectId, anchorQuery, 1);
    if (!embedded.isEmpty()) {
      return embedded.get(0);
    }

    throw new AnchorNotFoundException("No artifact found for anchor: " + anchorQuery);
  }

  /**
   * Get all version history for an artifact by description.
   */
  public List<ArtifactNode> getVersionHistory(UUID projectId, String description) {
    return artifactRepository.findVersionsByAnchorDescription(projectId, description).stream()
        .map(this::toArtifactNode)
        .filter(node -> node != null)
        .toList();
  }

  /**
   * Create a new artifact with a semantic anchor.
   */
  @Transactional
  public ArtifactEntity createWithAnchor(
      UUID projectId,
      UUID runId,
      UUID branchId,
      ArtifactEntity.Kind kind,
      SemanticAnchor anchor,
      Provenance provenance
  ) {
    log.info("Creating artifact with anchor: {}", anchor.description());

    // Check if an artifact with this anchor already exists
    var existing = artifactRepository.findCurrentByAnchorDescription(projectId, anchor.description());

    var entity = ArtifactEntity.builder()
        .projectId(projectId)
        .runId(runId)
        .branchId(branchId)
        .kind(kind)
        .status(ArtifactEntity.Status.SCHEDULED)
        .title(anchor.description())
        .anchorDescription(anchor.description())
        .anchorRole(anchor.role())
        .anchorTags(objectMapper.valueToTree(anchor.tags()))
        .version(existing.map(e -> e.getVersion() + 1).orElse(1))
        .derivedFrom(objectMapper.valueToTree(provenance.derivedFrom()))
        .build();

    entity = artifactRepository.save(entity);

    // Mark old version as superseded
    if (existing.isPresent()) {
      var old = existing.get();
      old.setSupersededById(entity.getId());
      artifactRepository.save(old);

      // Create SUPERSEDES relation
      var relation = ArtifactRelationEntity.builder()
          .sourceArtifactId(old.getId())
          .targetArtifactId(entity.getId())
          .relationType(RelationType.SUPERSEDES)
          .build();
      relationRepository.save(relation);
    }

    // Create DERIVED_FROM relations
    for (UUID parentId : provenance.derivedFrom()) {
      var relation = ArtifactRelationEntity.builder()
          .sourceArtifactId(parentId)
          .targetArtifactId(entity.getId())
          .relationType(RelationType.DERIVED_FROM)
          .build();
      relationRepository.save(relation);
    }

    return entity;
  }

  /**
   * Add a semantic anchor to an existing artifact and create embedding.
   */
  @Transactional
  public void updateArtifactAnchor(UUID artifactId, SemanticAnchor anchor) {
    var entity = artifactRepository.findById(artifactId)
        .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));

    entity.setAnchorDescription(anchor.description());
    entity.setAnchorRole(anchor.role());
    entity.setAnchorTags(objectMapper.valueToTree(anchor.tags()));
    artifactRepository.save(entity);

    // Update or create embedding
    updateAnchorEmbedding(artifactId, anchor);
  }

  /**
   * Update or create the embedding for an artifact's anchor.
   */
  @Transactional
  public void updateAnchorEmbedding(UUID artifactId, SemanticAnchor anchor) {
    var embedding = embeddingService.generateEmbedding(anchor.description());
    if (embedding == null) {
      log.warn("Failed to generate embedding for anchor: {}", anchor.description());
      return;
    }

    // Delete existing embedding
    embeddingRepository.deleteByArtifactId(artifactId);

    // Create new embedding
    var entity = com.example.hypocaust.db.AnchorEmbeddingEntity.builder()
        .artifactId(artifactId)
        .embedding(embedding)
        .anchorHash(anchor.computeHash())
        .build();
    embeddingRepository.save(entity);
  }

  /**
   * Convert an ArtifactEntity to an ArtifactNode domain object.
   */
  private ArtifactNode toArtifactNode(ArtifactEntity entity) {
    // Skip artifacts without anchor descriptions
    if (entity.getAnchorDescription() == null) {
      return null;
    }

    // Build semantic anchor
    Set<String> tags = new HashSet<>();
    if (entity.getAnchorTags() != null && entity.getAnchorTags().isArray()) {
      for (JsonNode tag : entity.getAnchorTags()) {
        tags.add(tag.asText());
      }
    }
    var anchor = new SemanticAnchor(
        entity.getAnchorDescription(),
        entity.getAnchorRole(),
        tags
    );

    // Build provenance
    List<UUID> derivedFrom = new ArrayList<>();
    if (entity.getDerivedFrom() != null && entity.getDerivedFrom().isArray()) {
      for (JsonNode id : entity.getDerivedFrom()) {
        try {
          derivedFrom.add(UUID.fromString(id.asText()));
        } catch (Exception e) {
          log.warn("Invalid UUID in derivedFrom for artifact {}: {}", entity.getId(), id);
        }
      }
    }
    var provenance = new Provenance(
        null, // operator name not stored in entity
        Map.of(), // inputs not stored
        entity.getRunId(),
        derivedFrom
    );

    // Map kind
    ArtifactKind kind = switch (entity.getKind()) {
      case STRUCTURED_JSON -> ArtifactKind.STRUCTURED_JSON;
      case IMAGE -> ArtifactKind.IMAGE;
      case PDF -> ArtifactKind.PDF;
      case AUDIO -> ArtifactKind.AUDIO;
      case VIDEO -> ArtifactKind.VIDEO;
    };

    return new ArtifactNode(
        entity.getId(),
        anchor,
        kind,
        entity.getContent(),
        entity.getStorageKey(),
        provenance,
        entity.getVersion() != null ? entity.getVersion() : 1,
        entity.getSupersededById()
    );
  }
}
