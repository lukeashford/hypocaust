package com.example.hypocaust.exception;

import com.example.hypocaust.domain.ArtifactKind;
import lombok.Getter;

/**
 * Thrown when attempting to edit an artifact with a different type than the existing one.
 */
@Getter
public class ArtifactTypeMismatchException extends RuntimeException {

  private final String artifactName;
  private final ArtifactKind expectedKind;
  private final ArtifactKind actualKind;

  public ArtifactTypeMismatchException(String artifactName, ArtifactKind expectedKind,
      ArtifactKind actualKind) {
    super(String.format("Type mismatch for artifact '%s': expected %s but got %s",
        artifactName, expectedKind, actualKind));
    this.artifactName = artifactName;
    this.expectedKind = expectedKind;
    this.actualKind = actualKind;
  }
}
