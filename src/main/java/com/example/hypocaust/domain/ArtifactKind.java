package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Content type category of an artifact", enumAsRef = true)
public enum ArtifactKind {
  /** JSON data, text, analysis results, or other structured content. */
  STRUCTURED_JSON,
  /** Visual content (PNG, JPG, WebP, etc.). */
  IMAGE,
  /** Document files including PDFs and presentations. */
  PDF,
  /** Sound files and recordings. */
  AUDIO,
  /** Video content. */
  VIDEO
}
