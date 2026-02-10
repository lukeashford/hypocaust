package com.example.hypocaust.domain;

/**
 * The type of content an artifact represents.
 * This enum aligns with ArtifactEntity.Kind but exists at the domain level
 * for use in domain records without JPA dependencies.
 */
public enum ArtifactKind {
  /**
   * JSON data, text, analysis results, or other structured content.
   */
  STRUCTURED_JSON,

  /**
   * Visual content (PNG, JPG, WebP, etc.).
   */
  IMAGE,

  /**
   * Document files including PDFs and presentations.
   */
  PDF,

  /**
   * Sound files and recordings.
   */
  AUDIO,

  /**
   * Video content.
   */
  VIDEO
}
