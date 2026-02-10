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
   * Generated, but not yet saved.
   */
  CREATED,

  /**
   * Saved in our storage.
   */
  MANIFESTED,

  /**
   * Generation or materialization failed.
   */
  FAILED

}
