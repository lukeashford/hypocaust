package com.example.the_machine.repo

import com.example.the_machine.domain.OperatorEmbedding
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository for operator embeddings with vector similarity search capabilities.
 * Provides methods to store and retrieve operator embeddings for semantic search.
 */
@Repository
interface OperatorEmbeddingRepository : JpaRepository<OperatorEmbedding, Long> {

  /**
   * Finds an operator embedding by its exact operator name.
   *
   * @param operatorName the name of the operator to search for
   * @return optional operator embedding if found
   */
  fun findByOperatorName(operatorName: String): Optional<OperatorEmbedding>

  /**
   * Finds operator embeddings by similarity to a query embedding using cosine distance.
   * Results are ordered by similarity (closest first).
   *
   * @param queryEmbedding the query embedding vector to compare against
   * @param pageable pagination parameters to limit results
   * @return list of operator embeddings ordered by similarity
   */
  @Query("SELECT o FROM OperatorEmbedding o ORDER BY vector_cosine_distance(o.embedding, :queryEmbedding) ASC")
  fun findTopByEmbeddingSimilarity(
    @Param("queryEmbedding") queryEmbedding: FloatArray,
    pageable: Pageable
  ): List<OperatorEmbedding>
}