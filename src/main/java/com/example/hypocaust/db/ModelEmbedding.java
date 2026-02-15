package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing embeddings for RAG platform/model documents.
 */
@Entity
@Table(name = "model_embeddings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ModelEmbedding extends AbstractEmbedding {

  @Column(nullable = false)
  private String owner;

  @Column(name = "model_id", nullable = false)
  private String modelId;

  @Column(columnDefinition = "text", nullable = false)
  private String description;

  @Column(name = "best_practices", columnDefinition = "text", nullable = false)
  private String bestPractices;

  @Column(nullable = false)
  private String tier;

  @Builder
  public ModelEmbedding(String name, float[] embedding, String hash, String owner,
      String modelId, String description, String bestPractices, String tier) {
    super(name, embedding, hash);
    this.owner = owner;
    this.modelId = modelId;
    this.description = description;
    this.bestPractices = bestPractices;
    this.tier = tier;
  }

  public void update(String newHash, float[] newEmbedding, String owner, String modelId,
      String description, String bestPractices, String tier) {
    super.update(newHash, newEmbedding);
    this.owner = owner;
    this.modelId = modelId;
    this.description = description;
    this.bestPractices = bestPractices;
    this.tier = tier;
  }
}
