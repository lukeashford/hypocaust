package com.example.hypocaust.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing embeddings for RAG platform/model documents.
 */
@Entity
@Table(name = "platform_embeddings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformEmbedding extends AbstractEmbedding {

  @Builder
  public PlatformEmbedding(String name, float[] embedding, String hash, String text) {
    super(name, embedding, hash, text);
  }
}
