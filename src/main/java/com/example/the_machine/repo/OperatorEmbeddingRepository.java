package com.example.the_machine.repo;

import com.example.the_machine.db.OperatorEmbedding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for operator embeddings with vector similarity search capabilities. Provides methods
 * to store and retrieve operator embeddings for semantic search.
 */
@Repository
public interface OperatorEmbeddingRepository extends JpaRepository<OperatorEmbedding, Long> {

  /**
   * Finds an operator embedding by its exact operator name.
   *
   * @param operatorName the name of the operator to search for
   * @return optional operator embedding if found
   */
  Optional<OperatorEmbedding> findByOperatorName(String operatorName);

  /**
   * Finds operator embeddings by similarity to a query embedding using cosine distance. Results are
   * ordered by similarity (closest first).
   *
   * @param queryEmbedding the query embedding vector to compare against
   * @param pageable pagination parameters to limit results
   * @return list of operator embeddings ordered by similarity
   */
  @Query("SELECT o FROM OperatorEmbedding o ORDER BY vector_cosine_distance(o.embedding, :queryEmbedding) ASC")
  List<OperatorEmbedding> findTopByEmbeddingSimilarity(
      @Param("queryEmbedding") float[] queryEmbedding, Pageable pageable);
}