package com.example.hypocaust.domain;

import java.util.Set;

/**
 * A semantic anchor is a rich description that serves as an artifact's natural language identity.
 * It enables natural language references like "make the dog blonde" to resolve to the correct artifact.
 *
 * <p>The anchor is the stable identity across versions - content changes, but the anchor persists.
 *
 * @param description Human-readable description of the artifact (e.g., "A golden retriever wearing a top hat")
 * @param role Optional role within the project (e.g., "hero-image", "background-music", "opening-scene")
 * @param tags Searchable facets for the artifact (e.g., "dog", "park", "whimsical")
 */
public record SemanticAnchor(
    String description,
    String role,
    Set<String> tags
) {

  public SemanticAnchor {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Semantic anchor description cannot be null or blank");
    }
    tags = tags != null ? Set.copyOf(tags) : Set.of();
  }

  /**
   * Create a semantic anchor with just a description.
   */
  public static SemanticAnchor of(String description) {
    return new SemanticAnchor(description, null, Set.of());
  }

  /**
   * Create a semantic anchor with description and role.
   */
  public static SemanticAnchor of(String description, String role) {
    return new SemanticAnchor(description, role, Set.of());
  }

  /**
   * Create a semantic anchor with all fields.
   */
  public static SemanticAnchor of(String description, String role, Set<String> tags) {
    return new SemanticAnchor(description, role, tags);
  }

  /**
   * Compute a stable hash for this anchor to use as an index key.
   * Uses the description as the primary identifier.
   */
  public String computeHash() {
    return Integer.toHexString(description.toLowerCase().hashCode());
  }
}
