package com.example.hypocaust.repo;

import com.example.hypocaust.db.ModelEmbedding;
import com.example.hypocaust.domain.ArtifactKind;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelEmbeddingRepository extends JpaRepository<ModelEmbedding, UUID> {

  @Query("""
           select d from ModelEmbedding d
           where (select count(i) from d.inputs i where i not in :inputs) = 0
             and (select count(distinct os.kind) from d.outputs os where os.kind in :outputs) = :#{#outputs.size()}
           order by cosine_distance(d.embedding, :queryEmbedding)
      """)
  List<ModelEmbedding> findTopByInputsAndSimilarity(
      @Param("queryEmbedding") float[] queryEmbedding,
      @Param("inputs") Set<ArtifactKind> inputs,
      @Param("outputs") Set<ArtifactKind> outputs,
      Pageable pageable);
}
