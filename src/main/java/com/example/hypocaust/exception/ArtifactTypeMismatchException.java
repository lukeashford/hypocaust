package com.example.hypocaust.exception;

import com.example.hypocaust.db.ArtifactEntity.Kind;
import lombok.Getter;

/**
 * Thrown when attempting to edit an artifact with a different type than the existing one.
 */
@Getter
public class ArtifactTypeMismatchException extends RuntimeException {

  private final String artifactName;
  private final Kind expectedKind;
  private final Kind actualKind;

  public ArtifactTypeMismatchException(String artifactName, Kind expectedKind, Kind actualKind) {
    super(String.format("Type mismatch for artifact '%s': expected %s but got %s",
        artifactName, expectedKind, actualKind));
    this.artifactName = artifactName;
    this.expectedKind = expectedKind;
    this.actualKind = actualKind;
  }
}
