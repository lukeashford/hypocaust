package com.example.the_machine.domain

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.*

@Entity
@Table(name = "artifact")
data class ArtifactEntity(
  /**
   * Unique identifier for this artifact
   */
  @Id
  val id: UUID? = null,

  /**
   * The thread this artifact belongs to - required for all artifacts
   */
  @Column(nullable = false)
  val threadId: UUID? = null,

  /**
   * The run that generated this artifact - optional for user-uploaded artifacts
   */
  val runId: UUID? = null,

  /**
   * The type of content this artifact represents - STRUCTURED_JSON: JSON data, text, analysis
   * results - IMAGE: Visual content (PNG, JPG, etc.) - PDF: Document files including presentations
   * - AUDIO: Sound files and recordings - VIDEO: Video content
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val kind: Kind? = null,

  /**
   * The workflow stage this artifact represents - PLAN: Initial planning and strategy artifacts -
   * ANALYSIS: Research and analysis results - SCRIPT: Written content, copy, scripts - IMAGES:
   * Visual assets and graphics - DECK: Final presentations and documents
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val stage: Stage? = null,

  /**
   * Current processing status of the artifact - PENDING: Queued for generation - RUNNING: Currently
   * being generated - DONE: Successfully completed - FAILED: Generation failed
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val status: Status? = null,

  /**
   * Human-readable title or description of the artifact
   */
  val title: String? = null,

  /**
   * MIME type for proper content handling (image/png, application/pdf, etc.)
   */
  val mime: String? = null,

  /**
   * Storage location for file-based artifacts (images, PDFs, etc.)
   */
  val storageKey: String? = null,

  /**
   * Inline content for structured artifacts (JSON, text) Used when the artifact content can be
   * stored directly in the database
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val content: JsonNode? = null,

  /**
   * Technical metadata about the artifact Examples: image dimensions, video duration, file size,
   * generation parameters
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val metadata: JsonNode? = null,

  /**
   * When this artifact was created
   */
  @Column(nullable = false)
  val createdAt: Instant? = null,

  /**
   * ID of the artifact that this one superseded/replaced, if any
   */
  val supersededById: UUID? = null
) {

  enum class Kind {
    STRUCTURED_JSON, IMAGE, PDF, AUDIO, VIDEO
  }

  enum class Stage {
    PLAN, ANALYSIS, SCRIPT, IMAGES, DECK
  }

  enum class Status {
    PENDING, RUNNING, DONE, FAILED
  }

}