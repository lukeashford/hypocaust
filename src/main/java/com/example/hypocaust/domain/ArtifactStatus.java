package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Processing status of an artifact", enumAsRef = true)
public enum ArtifactStatus {
  /**
   * Still being generated — show a skeleton placeholder in the UI.
   */
  GESTATING,
  /**
   * User-uploaded but not yet analyzed — name, title, and description are placeholders.
   */
  UPLOADED,
  /**
   * Fully persisted — inline content or storage key is available.
   */
  MANIFESTED,
  /**
   * Generation or storage failed.
   */
  FAILED,
  /**
   * Generation was cancelled.
   */
  CANCELLED
}
