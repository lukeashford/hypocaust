package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  /**
   * Find all distinct artifact names for a project.
   */
  @Query("SELECT DISTINCT a.name FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.name IS NOT NULL")
  List<String> findDistinctNamesByProjectId(@Param("projectId") UUID projectId);
}