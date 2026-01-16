package com.example.hypocaust.repo;

import com.example.hypocaust.db.WorkflowEmbedding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowEmbeddingRepository extends JpaRepository<WorkflowEmbedding, UUID> {

  Optional<WorkflowEmbedding> findByName(String name);

  @Query("""
      select d
      from WorkflowEmbedding d
      order by cosine_distance(d.embedding, :queryEmbedding)
      """)
  List<WorkflowEmbedding> findTopByEmbeddingSimilarity(
      @Param("queryEmbedding") float[] queryEmbedding, Pageable pageable);
}
