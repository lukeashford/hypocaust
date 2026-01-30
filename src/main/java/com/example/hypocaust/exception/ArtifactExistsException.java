package com.example.hypocaust.exception;

import lombok.Getter;

/**
 * Thrown when attempting to create an artifact with a fileName that already exists.
 */
@Getter
public class ArtifactExistsException extends RuntimeException {

  private final String artifactName;

  public ArtifactExistsException(String artifactName) {
    super("Artifact already exists: " + artifactName);
    this.artifactName = artifactName;
  }
}
