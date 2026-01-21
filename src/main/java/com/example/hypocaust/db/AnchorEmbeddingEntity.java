package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stores embeddings for artifact semantic anchors to enable semantic search.
 */
@Entity
@Table(name = "anchor_embedding")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AnchorEmbeddingEntity extends BaseEntity {

  /**
   * The artifact this embedding belongs to.
   */
  @Column(nullable = false)
  private UUID artifactId;

  /**
   * The embedding vector for semantic search.
   * Note: Stored as float[] and handled by pgvector.
   */
  @Column(columnDefinition = "vector(1536)", nullable = false)
  private float[] embedding;

  /**
   * Hash of the anchor description for quick lookups.
   */
  @Column(nullable = false)
  private String anchorHash;
}
