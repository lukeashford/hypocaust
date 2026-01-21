package com.example.hypocaust.repo;

import com.example.hypocaust.db.AnchorEmbeddingEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnchorEmbeddingRepository extends JpaRepository<AnchorEmbeddingEntity, UUID> {

  /**
   * Find embedding by artifact ID.
   */
  Optional<AnchorEmbeddingEntity> findByArtifactId(UUID artifactId);

  /**
   * Find embeddings by anchor hash.
   */
  List<AnchorEmbeddingEntity> findByAnchorHash(String anchorHash);

  /**
   * Delete embedding for an artifact.
   */
  void deleteByArtifactId(UUID artifactId);

  /**
   * Semantic search on anchor embeddings using cosine similarity.
   * Returns artifact IDs ordered by similarity.
   *
   * @param embedding The query embedding
   * @param limit Maximum number of results
   * @return List of artifact IDs ordered by similarity
   */
  @Query(value = """
      SELECT ae.artifact_id
      FROM anchor_embedding ae
      ORDER BY ae.embedding <=> cast(:embedding as vector)
      LIMIT :limit
      """, nativeQuery = true)
  List<UUID> findSimilarArtifacts(
      @Param("embedding") float[] embedding,
      @Param("limit") int limit);

  /**
   * Semantic search on anchor embeddings for artifacts in a specific project.
   *
   * @param embedding The query embedding
   * @param projectId The project to search within
   * @param limit Maximum number of results
   * @return List of artifact IDs ordered by similarity
   */
  @Query(value = """
      SELECT ae.artifact_id
      FROM anchor_embedding ae
      JOIN artifact a ON ae.artifact_id = a.id
      WHERE a.project_id = :projectId
      ORDER BY ae.embedding <=> cast(:embedding as vector)
      LIMIT :limit
      """, nativeQuery = true)
  List<UUID> findSimilarArtifactsInProject(
      @Param("embedding") float[] embedding,
      @Param("projectId") UUID projectId,
      @Param("limit") int limit);
}
