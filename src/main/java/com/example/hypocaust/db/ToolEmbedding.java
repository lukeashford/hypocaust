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
 * JPA entity for tool embeddings stored in pgvector for semantic search.
 */
@Entity
@Table(name = "tool_embeddings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ToolEmbedding extends BaseEntity {

  @Column(name = "tool_name", unique = true, nullable = false)
  private String toolName;

  @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
  @JdbcTypeCode(SqlTypes.VECTOR)
  @Array(length = 1536)
  private float[] embedding;

  @Column(nullable = false, length = 64)
  private String hash;

  public void updateEmbedding(float[] newEmbedding, String newHash) {
    this.embedding = newEmbedding == null ? null : newEmbedding.clone();
    this.hash = newHash;
  }
}
