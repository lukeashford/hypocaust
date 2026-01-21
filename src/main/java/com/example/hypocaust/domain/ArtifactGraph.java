package com.example.hypocaust.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The artifact graph represents the persistent state of all artifacts within a project.
 * It provides methods for querying artifacts by various criteria including semantic anchors.
 *
 * <p>The graph is immutable at the domain level - mutations are tracked separately
 * in {@link PendingChanges} and applied atomically at commit time.
 *
 * @param nodes All artifacts in the graph, indexed by ID
 * @param anchorIndex Mapping from anchor hash to current (latest) version ID
 * @param relations All derivation/supersedes/reference edges
 */
public record ArtifactGraph(
    Map<UUID, ArtifactNode> nodes,
    Map<String, UUID> anchorIndex,
    List<ArtifactRelation> relations
) {

  public ArtifactGraph {
    nodes = nodes != null ? Map.copyOf(nodes) : Map.of();
    anchorIndex = anchorIndex != null ? Map.copyOf(anchorIndex) : Map.of();
    relations = relations != null ? List.copyOf(relations) : List.of();
  }

  /**
   * Create an empty artifact graph.
   */
  public static ArtifactGraph empty() {
    return new ArtifactGraph(Map.of(), Map.of(), List.of());
  }

  /**
   * Get an artifact by its ID.
   */
  public Optional<ArtifactNode> getById(UUID id) {
    return Optional.ofNullable(nodes.get(id));
  }

  /**
   * Find an artifact by exact anchor match.
   */
  public Optional<ArtifactNode> findByAnchor(SemanticAnchor anchor) {
    String hash = anchor.computeHash();
    UUID currentId = anchorIndex.get(hash);
    if (currentId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(nodes.get(currentId));
  }

  /**
   * Find an artifact by role.
   */
  public Optional<ArtifactNode> findByRole(String role) {
    if (role == null || role.isBlank()) {
      return Optional.empty();
    }
    return nodes.values().stream()
        .filter(node -> role.equals(node.anchor().role()))
        .findFirst();
  }

  /**
   * Find artifacts matching a description query.
   * This is a simple substring match - in production, semantic search would be used.
   *
   * @param query The description query to search for
   * @return List of matching artifacts, ordered by relevance
   */
  public List<ArtifactNode> findByDescription(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    String lowerQuery = query.toLowerCase();

    // Get all current versions (those in anchorIndex)
    return anchorIndex.values().stream()
        .map(nodes::get)
        .filter(node -> node != null)
        .filter(node -> {
          String desc = node.anchor().description().toLowerCase();
          // Check if description contains the query
          return desc.contains(lowerQuery) ||
              // Or if any tag matches
              node.anchor().tags().stream()
                  .anyMatch(tag -> tag.toLowerCase().contains(lowerQuery));
        })
        .collect(Collectors.toList());
  }

  /**
   * Get all current (non-superseded) artifacts.
   */
  public List<ArtifactNode> getCurrentArtifacts() {
    return anchorIndex.values().stream()
        .map(nodes::get)
        .filter(node -> node != null)
        .collect(Collectors.toList());
  }

  /**
   * Get all versions of an artifact by anchor hash.
   */
  public List<ArtifactNode> getVersionHistory(String anchorHash) {
    UUID currentId = anchorIndex.get(anchorHash);
    if (currentId == null) {
      return List.of();
    }

    List<ArtifactNode> history = new ArrayList<>();
    ArtifactNode current = nodes.get(currentId);
    while (current != null) {
      history.add(current);
      current = current.supersedes() != null ? nodes.get(current.supersedes()) : null;
    }
    return history;
  }

  /**
   * Get relations where the given artifact is the source.
   */
  public List<ArtifactRelation> getOutgoingRelations(UUID artifactId) {
    return relations.stream()
        .filter(r -> r.sourceId().equals(artifactId))
        .collect(Collectors.toList());
  }

  /**
   * Get relations where the given artifact is the target.
   */
  public List<ArtifactRelation> getIncomingRelations(UUID artifactId) {
    return relations.stream()
        .filter(r -> r.targetId().equals(artifactId))
        .collect(Collectors.toList());
  }

  /**
   * Check if the graph is empty.
   */
  public boolean isEmpty() {
    return nodes.isEmpty();
  }

  /**
   * Get the number of current artifacts (not counting superseded versions).
   */
  public int currentArtifactCount() {
    return anchorIndex.size();
  }

  /**
   * Get the total number of artifact nodes (including all versions).
   */
  public int totalNodeCount() {
    return nodes.size();
  }

  /**
   * Create a builder for constructing a new graph from this one.
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Builder for constructing artifact graphs with mutations.
   */
  public static class Builder {
    private final Map<UUID, ArtifactNode> nodes;
    private final Map<String, UUID> anchorIndex;
    private final List<ArtifactRelation> relations;

    public Builder() {
      this.nodes = new HashMap<>();
      this.anchorIndex = new HashMap<>();
      this.relations = new ArrayList<>();
    }

    public Builder(ArtifactGraph base) {
      this.nodes = new HashMap<>(base.nodes());
      this.anchorIndex = new HashMap<>(base.anchorIndex());
      this.relations = new ArrayList<>(base.relations());
    }

    /**
     * Add a new artifact node.
     */
    public Builder addNode(ArtifactNode node) {
      nodes.put(node.id(), node);
      anchorIndex.put(node.getAnchorHash(), node.id());
      return this;
    }

    /**
     * Add a new version that supersedes an existing artifact.
     */
    public Builder addVersion(ArtifactNode newVersion) {
      nodes.put(newVersion.id(), newVersion);
      anchorIndex.put(newVersion.getAnchorHash(), newVersion.id());
      if (newVersion.supersedes() != null) {
        relations.add(ArtifactRelation.supersedes(newVersion.supersedes(), newVersion.id()));
      }
      return this;
    }

    /**
     * Add a relation.
     */
    public Builder addRelation(ArtifactRelation relation) {
      relations.add(relation);
      return this;
    }

    /**
     * Build the immutable graph.
     */
    public ArtifactGraph build() {
      return new ArtifactGraph(nodes, anchorIndex, relations);
    }
  }
}
