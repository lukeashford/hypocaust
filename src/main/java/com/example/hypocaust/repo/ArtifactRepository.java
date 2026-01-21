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
   * Find all artifacts for a specific project, ordered by creation time
   */
  List<ArtifactEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  /**
   * Find all artifacts for a branch, ordered by creation time.
   */
  List<ArtifactEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

  /**
   * Find artifacts by project that have not been superseded.
   * These are the "current" artifacts.
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.supersededById IS NULL")
  List<ArtifactEntity> findCurrentArtifactsByProject(@Param("projectId") UUID projectId);

  /**
   * Find artifacts by branch that have not been superseded.
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.branchId = :branchId AND a.supersededById IS NULL")
  List<ArtifactEntity> findCurrentArtifactsByBranch(@Param("branchId") UUID branchId);

  /**
   * Find artifact by anchor role within a project.
   */
  Optional<ArtifactEntity> findByProjectIdAndAnchorRoleAndSupersededByIdIsNull(
      UUID projectId, String anchorRole);

  /**
   * Find artifact by anchor role within a branch.
   */
  Optional<ArtifactEntity> findByBranchIdAndAnchorRoleAndSupersededByIdIsNull(
      UUID branchId, String anchorRole);

  /**
   * Full-text search on anchor descriptions.
   */
  @Query(value = """
      SELECT * FROM artifact
      WHERE project_id = :projectId
        AND anchor_description IS NOT NULL
        AND superseded_by_id IS NULL
        AND to_tsvector('english', anchor_description) @@ plainto_tsquery('english', :query)
      ORDER BY ts_rank(to_tsvector('english', anchor_description), plainto_tsquery('english', :query)) DESC
      """, nativeQuery = true)
  List<ArtifactEntity> searchByAnchorDescription(
      @Param("projectId") UUID projectId,
      @Param("query") String query);

  /**
   * Find all versions of an artifact by the same anchor description.
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.anchorDescription = :description ORDER BY a.version DESC")
  List<ArtifactEntity> findVersionsByAnchorDescription(
      @Param("projectId") UUID projectId,
      @Param("description") String description);

  /**
   * Find the latest version of an artifact by anchor description.
   */
  @Query("SELECT a FROM ArtifactEntity a WHERE a.projectId = :projectId AND a.anchorDescription = :description AND a.supersededById IS NULL")
  Optional<ArtifactEntity> findCurrentByAnchorDescription(
      @Param("projectId") UUID projectId,
      @Param("description") String description);
}