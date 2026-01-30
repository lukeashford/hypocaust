package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {

  /**
   * Find all artifacts for a specific project, ordered by creation time.
   */
  List<ArtifactEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  /**
   * Find artifact by TaskExecution ID and fileName.
   */
  Optional<ArtifactEntity> findByTaskExecutionIdAndFileName(UUID taskExecutionId, String name);

  /**
   * Find all artifacts created in a specific TaskExecution.
   */
  List<ArtifactEntity> findByTaskExecutionId(UUID taskExecutionId);

  /**
   * Find all artifacts with a given fileName in a project (all versions).
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.fileName = :fileName ORDER BY a.createdAt DESC")
  List<ArtifactEntity> findByProjectIdAndFileName(@Param("projectId") UUID projectId,
      @Param("fileName") String fileName);

  /**
   * Find non-deleted artifacts for a project.
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.status != 'DELETED' ORDER BY a.createdAt DESC")
  List<ArtifactEntity> findActiveByProjectId(@Param("projectId") UUID projectId);

  /**
   * Find all artifacts with a given fileName in a project that are not deleted (current versions).
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.fileName = :fileName AND a.status != 'DELETED' ORDER BY a.createdAt DESC")
  List<ArtifactEntity> findActiveByProjectIdAndFileName(@Param("projectId") UUID projectId,
      @Param("fileName") String fileName);

  /**
   * Find all distinct artifact names for a project.
   */
  @Query("SELECT DISTINCT a.fileName FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.fileName IS NOT NULL")
  List<String> findDistinctNamesByProjectId(@Param("projectId") UUID projectId);

  /**
   * Check if an artifact fileName exists in a project.
   */
  @Query("SELECT COUNT(a) > 0 FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.fileName = :fileName")
  boolean existsByProjectIdAndFileName(@Param("projectId") UUID projectId,
      @Param("fileName") String fileName);
}
