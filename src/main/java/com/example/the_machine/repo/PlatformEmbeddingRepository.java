package com.example.the_machine.repo;

import com.example.the_machine.db.PlatformEmbedding;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformEmbeddingRepository extends JpaRepository<PlatformEmbedding, UUID> {

  Optional<PlatformEmbedding> findByName(String name);

  @Query("""
      select d
      from PlatformEmbedding d
      order by cosine_distance(d.embedding, :queryEmbedding)
      """)
  List<PlatformEmbedding> findTopByEmbeddingSimilarity(
      @Param("queryEmbedding") float[] queryEmbedding, Pageable pageable);
}
