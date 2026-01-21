package com.example.hypocaust.db;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "artifact")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ArtifactEntity extends BaseEntity {

  /**
   * The project this artifact belongs to - required for all artifacts
   */
  @Column(nullable = false)
  private UUID projectId;

  /**
   * The run that generated this artifact - optional for user-uploaded artifacts
   */
  private UUID runId;

  /**
   * The branch this artifact was created on
   */
  private UUID branchId;

  /**
   * The commit that created this artifact
   */
  private UUID commitId;

  /**
   * The type of content this artifact represents - STRUCTURED_JSON: JSON data, text, analysis
   * results - IMAGE: Visual content (PNG, JPG, etc.) - PDF: Document files including presentations
   * - AUDIO: Sound files and recordings - VIDEO: Video content
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kind kind;

  /**
   * Current processing status of the artifact - PENDING: Queued for generation - RUNNING: Currently
   * being generated - DONE: Successfully completed - FAILED: Generation failed
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  /**
   * Human-readable title or description of the artifact
   */
  private String title;

  /**
   * Subtitle displayed below the title
   */
  private String subtitle;

  /**
   * Alt text for image artifacts, used for accessibility
   */
  private String alt;

  /**
   * MIME type for proper content handling (image/png, application/pdf, etc.)
   */
  private String mime;

  /**
   * Storage location for file-based artifacts (images, PDFs, etc.)
   */
  private String storageKey;

  /**
   * Inline content for structured artifacts (JSON, text) Used when the artifact content can be
   * stored directly in the database
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode content;

  /**
   * Technical metadata about the artifact Examples: image dimensions, video duration, file size,
   * generation parameters
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode metadata;

  /**
   * ID of the artifact that this one superseded/replaced, if any
   */
  private UUID supersededById;

  // =====================================================
  // Semantic Anchor Fields
  // =====================================================

  /**
   * Semantic anchor description - the natural language identity of this artifact.
   * Example: "A golden retriever wearing a top hat, sitting on a park bench"
   */
  private String anchorDescription;

  /**
   * Optional role within the project.
   * Example: "hero-image", "background-music", "opening-scene"
   */
  private String anchorRole;

  /**
   * Searchable tags for this artifact.
   * Example: ["dog", "park", "whimsical"]
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode anchorTags;

  /**
   * Version number within this anchor's version chain.
   * Starts at 1 for new artifacts, increments for each superseding version.
   */
  @Builder.Default
  private Integer version = 1;

  /**
   * UUIDs of artifacts this artifact was derived from.
   * Stored as JSON array of UUID strings.
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode derivedFrom;

  public enum Kind {
    STRUCTURED_JSON, IMAGE, PDF, AUDIO, VIDEO
  }

  public enum Status {
    SCHEDULED, CREATED, CANCELLED
  }
}