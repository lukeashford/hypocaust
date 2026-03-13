package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactChunkEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactChunkRepository extends JpaRepository<ArtifactChunkEntity, UUID> {

  @Query("""
      select c from ArtifactChunkEntity c
      where c.projectId = :projectId
      order by cosine_distance(c.embedding, :query)
      """)
  List<ArtifactChunkEntity> findByProjectSimilarity(
      @Param("projectId") UUID projectId,
      @Param("query") float[] query,
      Pageable pageable);

  @Query("""
      select c from ArtifactChunkEntity c
      where c.artifactId = :artifactId
      order by cosine_distance(c.embedding, :query)
      """)
  List<ArtifactChunkEntity> findByArtifactSimilarity(
      @Param("artifactId") UUID artifactId,
      @Param("query") float[] query,
      Pageable pageable);

  @Modifying
  void deleteByArtifactId(UUID artifactId);
}
