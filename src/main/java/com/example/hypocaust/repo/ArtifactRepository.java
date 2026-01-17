package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  /**
   * Find all artifacts for a specific project, ordered by creation time
   */
  List<ArtifactEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}