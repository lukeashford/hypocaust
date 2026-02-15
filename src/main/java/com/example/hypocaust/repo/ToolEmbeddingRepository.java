package com.example.hypocaust.repo;

import com.example.hypocaust.db.ToolEmbedding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolEmbeddingRepository extends JpaRepository<ToolEmbedding, UUID> {

  Optional<ToolEmbedding> findByToolName(String toolName);

  @Query("""
      select t
      from ToolEmbedding t
      order by cosine_distance(t.embedding, :queryEmbedding)
      """)
  List<ToolEmbedding> findTopByEmbeddingSimilarity(
      @Param("queryEmbedding") float[] queryEmbedding, Pageable pageable);
}
