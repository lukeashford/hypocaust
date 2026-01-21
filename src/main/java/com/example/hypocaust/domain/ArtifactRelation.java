package com.example.hypocaust.domain;

import java.util.UUID;

/**
 * Represents a directed edge in the artifact graph between two artifacts.
 *
 * @param sourceId The source artifact ID (the "from" artifact)
 * @param targetId The target artifact ID (the "to" artifact)
 * @param type The type of relationship
 */
public record ArtifactRelation(
    UUID sourceId,
    UUID targetId,
    RelationType type
) {

  public ArtifactRelation {
    if (sourceId == null) {
      throw new IllegalArgumentException("Source artifact ID cannot be null");
    }
    if (targetId == null) {
      throw new IllegalArgumentException("Target artifact ID cannot be null");
    }
    if (type == null) {
      throw new IllegalArgumentException("Relation type cannot be null");
    }
    if (sourceId.equals(targetId)) {
      throw new IllegalArgumentException("An artifact cannot have a relation with itself");
    }
  }

  /**
   * Create a DERIVED_FROM relation (target was derived from source).
   */
  public static ArtifactRelation derivedFrom(UUID sourceId, UUID targetId) {
    return new ArtifactRelation(sourceId, targetId, RelationType.DERIVED_FROM);
  }

  /**
   * Create a SUPERSEDES relation (target supersedes source).
   */
  public static ArtifactRelation supersedes(UUID sourceId, UUID targetId) {
    return new ArtifactRelation(sourceId, targetId, RelationType.SUPERSEDES);
  }

  /**
   * Create a REFERENCES relation (target references source).
   */
  public static ArtifactRelation references(UUID sourceId, UUID targetId) {
    return new ArtifactRelation(sourceId, targetId, RelationType.REFERENCES);
  }
}
