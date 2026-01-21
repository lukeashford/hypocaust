package com.example.hypocaust.repo;

import com.example.hypocaust.db.ArtifactRelationEntity;
import com.example.hypocaust.domain.RelationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactRelationRepository extends JpaRepository<ArtifactRelationEntity, UUID> {

  /**
   * Find all relations where the given artifact is the source.
   */
  List<ArtifactRelationEntity> findBySourceArtifactId(UUID sourceArtifactId);

  /**
   * Find all relations where the given artifact is the target.
   */
  List<ArtifactRelationEntity> findByTargetArtifactId(UUID targetArtifactId);

  /**
   * Find relations of a specific type from a source artifact.
   */
  List<ArtifactRelationEntity> findBySourceArtifactIdAndRelationType(
      UUID sourceArtifactId, RelationType relationType);

  /**
   * Find relations of a specific type to a target artifact.
   */
  List<ArtifactRelationEntity> findByTargetArtifactIdAndRelationType(
      UUID targetArtifactId, RelationType relationType);

  /**
   * Check if a specific relation exists.
   */
  boolean existsBySourceArtifactIdAndTargetArtifactIdAndRelationType(
      UUID sourceArtifactId, UUID targetArtifactId, RelationType relationType);

  /**
   * Delete all relations involving a specific artifact.
   */
  void deleteBySourceArtifactIdOrTargetArtifactId(UUID sourceArtifactId, UUID targetArtifactId);
}
