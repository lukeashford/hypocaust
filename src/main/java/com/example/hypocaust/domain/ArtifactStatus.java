package com.example.hypocaust.domain;

/**
 * Current processing status of an artifact.
 */
public enum ArtifactStatus {
  /**
   * In the works.
   */
  GESTATING,

  /**
   * Successfully completed.
   */
  CREATED,

  /**
   * Generation cancelled.
   */
  CANCELLED,

  /**
   * Soft-deleted.
   */
  DELETED
}
