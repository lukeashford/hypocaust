package com.example.hypocaust.repo;

import com.example.hypocaust.db.ModelEmbedding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelEmbeddingRepository extends JpaRepository<ModelEmbedding, UUID> {

  Optional<ModelEmbedding> findByName(String name);

  @Query("""
      select d
      from ModelEmbedding d
      order by cosine_distance(d.embedding, :queryEmbedding)
      """)
  List<ModelEmbedding> findTopByEmbeddingSimilarity(
      @Param("queryEmbedding") float[] queryEmbedding, Pageable pageable);
}
