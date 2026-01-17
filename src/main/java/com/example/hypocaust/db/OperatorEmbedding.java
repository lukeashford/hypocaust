package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA entity representing operator embeddings stored in the database for semantic search. Each
 * embedding corresponds to an operator's description and metadata converted to a vector
 * representation for similarity-based retrieval.
 */
@Entity
@Table(name = "operator_embeddings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OperatorEmbedding extends BaseEntity {

  @Column(name = "operator_name", unique = true, nullable = false)
  private String operatorName;

  @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
  @JdbcTypeCode(SqlTypes.VECTOR)
  @Array(length = 1536)
  private float[] embedding;

  @Column(nullable = false, length = 64)
  private String hash;

  /**
   * In-place update of the embedding and its hash. Replaces the array reference so JPA can detect
   * the change, and clones to avoid aliasing shared mutable state.
   */
  public void updateEmbedding(float[] newEmbedding, String newHash) {
    this.embedding = newEmbedding == null ? null : newEmbedding.clone();
    this.hash = newHash;
  }
}