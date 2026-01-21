package com.example.hypocaust.domain;

/**
 * Types of relationships between artifacts in the artifact graph.
 */
public enum RelationType {
  /**
   * The target artifact was derived from the source artifact.
   * Example: A video derived from an image.
   */
  DERIVED_FROM,

  /**
   * The target artifact supersedes (replaces) the source artifact.
   * They share the same semantic anchor but the target is a newer version.
   */
  SUPERSEDES,

  /**
   * The target artifact references the source artifact without being derived from it.
   * Example: A mood board that references but doesn't transform an image.
   */
  REFERENCES
}
