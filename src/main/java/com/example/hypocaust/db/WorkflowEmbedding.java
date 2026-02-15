package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing embeddings for RAG workflow documents.
 */
@Entity
@Table(name = "workflow_embeddings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class WorkflowEmbedding extends AbstractEmbedding {

  @Column(name = "text", columnDefinition = "text", nullable = false)
  private String text;

  @Builder
  public WorkflowEmbedding(String name, float[] embedding, String hash, String text) {
    super(name, embedding, hash);
    this.text = text;
  }

  public void update(String newText, String newHash, float[] newEmbedding) {
    super.update(newHash, newEmbedding);
    this.text = newText;
  }
}
