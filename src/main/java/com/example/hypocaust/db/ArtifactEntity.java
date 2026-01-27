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
   * The project this artifact belongs to - required for all artifacts.
   */
  @Column(nullable = false)
  private UUID projectId;

  /**
   * The TaskExecution that created this version of the artifact.
   */
  private UUID taskExecutionId;

  // =====================================================
  // Identity Fields
  // =====================================================

  /**
   * Semantic file name for this artifact.
   * Example: "protagonists_dog", "forest_background", "script"
   */
  @Column(length = 100)
  private String name;

  // =====================================================
  // Content Fields
  // =====================================================

  /**
   * The type of content this artifact represents.
   * - STRUCTURED_JSON: JSON data, text, analysis results
   * - IMAGE: Visual content (PNG, JPG, etc.)
   * - PDF: Document files including presentations
   * - AUDIO: Sound files and recordings
   * - VIDEO: Video content
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kind kind;

  /**
   * Storage location for file-based artifacts (images, PDFs, etc.)
   */
  private String storageKey;

  /**
   * Inline content for structured artifacts (JSON, text).
   * Used when the artifact content can be stored directly in the database.
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode content;

  // =====================================================
  // Metadata Fields
  // =====================================================

  /**
   * Full description of what's in the artifact.
   */
  @Column(columnDefinition = "text")
  private String description;

  /**
   * The prompt used to generate this artifact.
   */
  @Column(columnDefinition = "text")
  private String prompt;

  /**
   * The model used to generate this artifact.
   */
  @Column(length = 100)
  private String model;

  /**
   * Technical metadata about the artifact.
   * Examples: image dimensions, video duration, file size, generation parameters.
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode metadata;

  // =====================================================
  // Status Fields
  // =====================================================

  /**
   * Current processing status of the artifact.
   * - SCHEDULED: Queued for generation
   * - CREATED: Successfully completed
   * - CANCELLED: Generation cancelled
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  /**
   * Soft-delete flag. When true, this artifact is considered deleted
   * but the record is preserved for history.
   */
  @Builder.Default
  private boolean deleted = false;

  // =====================================================
  // Legacy Fields (kept for backwards compatibility during migration)
  // =====================================================

  /**
   * Human-readable title or description of the artifact.
   */
  private String title;

  /**
   * Subtitle displayed below the title.
   */
  private String subtitle;

  /**
   * Alt text for image artifacts, used for accessibility.
   */
  private String alt;

  /**
   * MIME type for proper content handling (image/png, application/pdf, etc.)
   */
  private String mime;

  public enum Kind {
    STRUCTURED_JSON, IMAGE, PDF, AUDIO, VIDEO
  }

  public enum Status {
    SCHEDULED, CREATED, CANCELLED
  }
}
