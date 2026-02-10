package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Processing status of an artifact", enumAsRef = true)
public enum ArtifactStatus {
  /** Still being generated — show a skeleton placeholder in the UI. */
  GESTATING,
  /** Generated but not yet persisted to storage. */
  CREATED,
  /** Fully persisted — inline content or URL is available. */
  MANIFESTED,
  /** Generation or storage failed. */
  FAILED
}
