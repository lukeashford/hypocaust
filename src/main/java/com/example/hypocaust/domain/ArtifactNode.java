package com.example.hypocaust.domain;

import java.util.UUID;

/**
 * Represents a node in the artifact graph - a single artifact with its semantic anchor,
 * content, provenance, and version information.
 *
 * @param id Unique identifier for this specific artifact version
 * @param anchor The semantic anchor providing natural language identity
 * @param kind The type of content this artifact represents
 * @param content The actual content (inline) or storage key (for files)
 * @param storageKey Storage location for file-based artifacts (null for inline content)
 * @param provenance Complete creation history for this artifact
 * @param version Version number within this anchor's version chain
 * @param supersedes ID of the previous version (same anchor), null if this is v1
 */
public record ArtifactNode(
    UUID id,
    SemanticAnchor anchor,
    ArtifactKind kind,
    Object content,
    String storageKey,
    Provenance provenance,
    int version,
    UUID supersedes
) {

  public ArtifactNode {
    if (id == null) {
      throw new IllegalArgumentException("Artifact node ID cannot be null");
    }
    if (anchor == null) {
      throw new IllegalArgumentException("Artifact node anchor cannot be null");
    }
    if (kind == null) {
      throw new IllegalArgumentException("Artifact node kind cannot be null");
    }
    if (version < 1) {
      throw new IllegalArgumentException("Artifact version must be at least 1");
    }
  }

  /**
   * Create a new first-version artifact node.
   */
  public static ArtifactNode create(
      UUID id,
      SemanticAnchor anchor,
      ArtifactKind kind,
      Object content,
      String storageKey,
      Provenance provenance
  ) {
    return new ArtifactNode(id, anchor, kind, content, storageKey, provenance, 1, null);
  }

  /**
   * Create a new version of this artifact that supersedes it.
   */
  public ArtifactNode newVersion(UUID newId, Object newContent, String newStorageKey, Provenance newProvenance) {
    return new ArtifactNode(
        newId,
        this.anchor,
        this.kind,
        newContent,
        newStorageKey,
        newProvenance,
        this.version + 1,
        this.id
    );
  }

  /**
   * Check if this artifact has inline content (vs file storage).
   */
  public boolean hasInlineContent() {
    return storageKey == null && content != null;
  }

  /**
   * Check if this artifact has file-based storage.
   */
  public boolean hasFileStorage() {
    return storageKey != null;
  }

  /**
   * Get the anchor hash for indexing.
   */
  public String getAnchorHash() {
    return anchor.computeHash();
  }
}
