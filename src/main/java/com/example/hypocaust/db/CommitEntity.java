package com.example.hypocaust.db;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents an immutable commit in the project's version history.
 * Each successful run produces a commit containing the delta of changes.
 */
@Entity
@Table(name = "commit")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CommitEntity extends BaseEntity {

  /**
   * The branch this commit belongs to.
   */
  @Column(nullable = false)
  private UUID branchId;

  /**
   * The previous commit on this branch.
   * Null for the first commit.
   */
  private UUID parentCommitId;

  /**
   * The run that produced this commit.
   */
  @Column(nullable = false)
  private UUID runId;

  /**
   * The verbatim task that was executed.
   * Stored for visualization and history purposes.
   */
  @Column(nullable = false)
  private String task;

  /**
   * When this commit was created.
   */
  @Column(nullable = false)
  private Instant timestamp;

  /**
   * The changes made in this commit.
   * Contains added, updated, and removed artifact IDs.
   */
  @Column(columnDefinition = "jsonb", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON)
  private JsonNode delta;

  /**
   * Check if this is the first commit on the branch.
   */
  public boolean isInitial() {
    return parentCommitId == null;
  }
}
