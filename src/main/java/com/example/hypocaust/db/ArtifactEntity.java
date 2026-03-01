package com.example.hypocaust.db;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.ArtifactStatus;
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

  // =====================================================
  // Identity Fields
  // =====================================================

  /**
   * The project this artifact belongs to - required for all artifacts.
   */
  @Column(nullable = false)
  private UUID projectId;

  /**
   * The TaskExecution that created this version of the artifact.
   */
  private UUID taskExecutionId;

  /**
   * Semantic file name for this artifact. Example: "protagonists_dog", "forest_background",
   * "script"
   */
  @Column(length = 100, nullable = false)
  private String name;

  // =====================================================
  // Content Fields
  // =====================================================

  /**
   * The type of content this artifact represents. - TEXT: Plain text or Markdown documents - IMAGE:
   * Visual content (PNG, JPG, etc.) - PDF: Portable Document Format files - AUDIO: Sound files and
   * recordings - VIDEO: Video content
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ArtifactKind kind;

  /**
   * Storage location for file-based artifacts (images, PDFs, etc.). Null for inline artifacts.
   */
  private String storageKey;

  /**
   * Inline content for textual artifacts (JSON, text). Used when the artifact content can be stored
   * directly in the database.
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode inlineContent;

  // =====================================================
  // Display Fields
  // =====================================================

  /**
   * Human-readable title for display in the UI. Distinct from 'name' which is the programmatic
   * identifier.
   */
  private String title;

  /**
   * Full description of what's in the artifact.
   */
  @Column(columnDefinition = "text")
  private String description;

  // =====================================================
  // Status Fields
  // =====================================================

  /**
   * Current processing status of the artifact. - SCHEDULED: Queued for generation - CREATED:
   * Successfully completed - CANCELLED: Generation cancelled
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ArtifactStatus status;

  // =====================================================
  // Metadata Fields
  // =====================================================

  /**
   * Technical metadata about the artifact. Examples: image dimensions, video duration, file size,
   * generation parameters.
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode metadata;

  /**
   * Technical MIME type of the artifact.
   */
  private String mimeType;

  /**
   * Error message describing why generation or storage failed (null if no error).
   */
  @Column(columnDefinition = "text")
  private String errorMessage;

}
