package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Content type category of an artifact", enumAsRef = true)
public enum ArtifactKind {
  /**
   * Visual content (PNG, JPG, WebP, etc.).
   */
  IMAGE,
  /**
   * Portable Document Format (PDF) files.
   */
  PDF,
  /**
   * Sound files and recordings.
   */
  AUDIO,
  /**
   * Video content.
   */
  VIDEO,
  /**
   * Text documents (non-PDF), e.g., plain text or Markdown.
   */
  TEXT,
  /**
   * Fallback for unknown or other types.
   */
  OTHER
}
