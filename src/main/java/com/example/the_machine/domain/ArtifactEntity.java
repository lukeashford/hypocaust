package com.example.the_machine.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
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
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactEntity {

  /**
   * Unique identifier for this artifact
   */
  @Id
  private UUID id;

  /**
   * The thread this artifact belongs to - required for all artifacts
   */
  @Column(nullable = false)
  private UUID threadId;

  /**
   * The run that generated this artifact - optional for user-uploaded artifacts
   */
  private UUID runId;

  /**
   * The type of content this artifact represents - STRUCTURED_JSON: JSON data, text, analysis
   * results - IMAGE: Visual content (PNG, JPG, etc.) - PDF: Document files including presentations
   * - AUDIO: Sound files and recordings - VIDEO: Video content
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kind kind;

  /**
   * The workflow stage this artifact represents - PLAN: Initial planning and strategy artifacts -
   * ANALYSIS: Research and analysis results - SCRIPT: Written content, copy, scripts - IMAGES:
   * Visual assets and graphics - DECK: Final presentations and documents
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Stage stage;

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
   * When this artifact was created
   */
  @Column(nullable = false)
  private Instant createdAt;

  /**
   * ID of the artifact that this one superseded/replaced, if any
   */
  private UUID supersededById;

  public enum Kind {
    STRUCTURED_JSON, IMAGE, PDF, AUDIO, VIDEO
  }

  public enum Stage {
    PLAN, ANALYSIS, SCRIPT, IMAGES, DECK
  }

  public enum Status {
    PENDING, RUNNING, DONE, FAILED
  }
}