package com.example.the_machine.db;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * JPA entity representing embeddings for RAG workflow documents.
 */
@Entity
@Table(name = "workflow_embeddings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowEmbedding extends AbstractEmbedding {

  @Builder
  public WorkflowEmbedding(String name, float[] embedding, String hash, String text) {
    super(name, embedding, hash, text);
  }
}
