package com.example.hypocaust.domain;

import java.util.UUID;

/**
 * Represents an update to an existing artifact (creating a new version).
 *
 * @param anchorHash The anchor hash identifying which artifact family was updated
 * @param oldVersionId The ID of the previous version
 * @param newVersionId The ID of the new version
 */
public record ArtifactUpdate(
    String anchorHash,
    UUID oldVersionId,
    UUID newVersionId
) {

  public ArtifactUpdate {
    if (anchorHash == null || anchorHash.isBlank()) {
      throw new IllegalArgumentException("Anchor hash cannot be null or blank");
    }
    if (oldVersionId == null) {
      throw new IllegalArgumentException("Old version ID cannot be null");
    }
    if (newVersionId == null) {
      throw new IllegalArgumentException("New version ID cannot be null");
    }
    if (oldVersionId.equals(newVersionId)) {
      throw new IllegalArgumentException("Old and new version IDs cannot be the same");
    }
  }
}
