package com.example.hypocaust.db;

import com.example.hypocaust.domain.RelationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a directed relationship between two artifacts.
 * Tracks derivation, versioning, and reference relationships.
 */
@Entity
@Table(name = "artifact_relation", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_artifact_id", "target_artifact_id", "relation_type"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ArtifactRelationEntity extends BaseEntity {

  /**
   * The source artifact (the "from" artifact).
   */
  @Column(nullable = false)
  private UUID sourceArtifactId;

  /**
   * The target artifact (the "to" artifact).
   */
  @Column(nullable = false)
  private UUID targetArtifactId;

  /**
   * The type of relationship.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RelationType relationType;
}
