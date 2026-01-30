package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@MappedSuperclass
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractEmbedding extends BaseEntity {

  protected static final int DIM = 1536;
  protected static final String VECTOR_DEF = "vector(1536)";

  @Column(name = "fileName", unique = true, nullable = false)
  protected String name;

  @Column(name = "embedding", nullable = false, columnDefinition = VECTOR_DEF)
  @JdbcTypeCode(SqlTypes.VECTOR)
  @Array(length = DIM)
  protected float[] embedding;

  @Column(nullable = false, length = 64)
  protected String hash;

  @Column(name = "text", columnDefinition = "text", nullable = false)
  protected String text;

  protected AbstractEmbedding(String name, float[] embedding, String hash, String text) {
    this.name = name;
    this.embedding = embedding;
    this.hash = hash;
    this.text = text;
  }

  /**
   * In-place update of the document text, hash and embedding. Replaces the array reference so JPA
   * can detect the change; clones to prevent aliasing external mutable arrays.
   */
  public void update(String newText, String newHash, float[] newEmbedding) {
    this.text = newText;
    this.hash = newHash;
    if (newEmbedding != null) {
      this.embedding = newEmbedding.clone();
    }
  }
}
