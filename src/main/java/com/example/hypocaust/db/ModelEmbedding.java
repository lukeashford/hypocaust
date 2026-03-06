package com.example.hypocaust.db;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.domain.OutputSpec;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Set;
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

  @Column(nullable = false)
  private String platform;

  @ElementCollection(targetClass = ArtifactKind.class, fetch = FetchType.EAGER)
  @CollectionTable(name = "model_embedding_inputs", joinColumns = @JoinColumn(name = "model_embedding_id"))
  @Column(name = "kind")
  @Enumerated(EnumType.STRING)
  private Set<ArtifactKind> inputs;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "model_embedding_outputs", joinColumns = @JoinColumn(name = "model_embedding_id"))
  private Set<OutputSpec> outputs;

  @Builder
  public ModelEmbedding(String name, float[] embedding, String hash, String owner,
      String modelId, String description, String bestPractices, String tier, String platform,
      Set<ArtifactKind> inputs, Set<OutputSpec> outputs) {
    super(name, embedding, hash);
    this.owner = owner;
    this.modelId = modelId;
    this.description = description;
    this.bestPractices = bestPractices;
    this.tier = tier;
    this.platform = platform != null ? platform : "REPLICATE";
    this.inputs = inputs;
    this.outputs = outputs;
  }

  public void update(String newHash, float[] newEmbedding, String owner, String modelId,
      String description, String bestPractices, String tier, String platform,
      Set<ArtifactKind> inputs, Set<OutputSpec> outputs) {
    super.update(newHash, newEmbedding);
    this.owner = owner;
    this.modelId = modelId;
    this.description = description;
    this.bestPractices = bestPractices;
    this.tier = tier;
    this.platform = platform != null ? platform : "REPLICATE";
    this.inputs = inputs;
    this.outputs = outputs;
  }
}
