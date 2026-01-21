package com.example.hypocaust.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Represents a branch in a project's version history.
 * Each project can have multiple branches for exploring alternative artifact versions.
 */
@Entity
@Table(name = "branch", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"project_id", "name"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BranchEntity extends BaseEntity {

  /**
   * The project this branch belongs to.
   */
  @Column(nullable = false)
  private UUID projectId;

  /**
   * Human-readable name of the branch.
   * Example: "main", "blonde-variant", "beach-background"
   */
  @Column(nullable = false)
  private String name;

  /**
   * The current head commit of this branch.
   * Null for newly created branches with no commits yet.
   */
  private UUID headCommitId;

  /**
   * The branch this was forked from.
   * Null for the initial main branch.
   */
  private UUID parentBranchId;

  /**
   * Check if this is the main branch.
   */
  public boolean isMain() {
    return "main".equals(name);
  }

  /**
   * Check if this branch has any commits.
   */
  public boolean hasCommits() {
    return headCommitId != null;
  }
}
