package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.stream.Collectors;

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
  OTHER;

  public static String toJsonArray() {
    return Arrays.stream(values())
        .map(k -> "\"" + k.name() + "\"")
        .collect(Collectors.joining(", ", "[", "]"));
  }
}
