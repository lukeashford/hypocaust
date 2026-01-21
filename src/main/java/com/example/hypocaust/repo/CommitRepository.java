package com.example.hypocaust.repo;

import com.example.hypocaust.db.CommitEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitRepository extends JpaRepository<CommitEntity, UUID> {

  /**
   * Find all commits for a branch, ordered by timestamp.
   */
  List<CommitEntity> findByBranchIdOrderByTimestampDesc(UUID branchId);

  /**
   * Find the commit for a specific run.
   */
  Optional<CommitEntity> findByRunId(UUID runId);

  /**
   * Find commits with a specific parent.
   */
  List<CommitEntity> findByParentCommitId(UUID parentCommitId);

  /**
   * Get the commit history chain starting from a commit.
   */
  @Query(value = """
      WITH RECURSIVE commit_chain AS (
        SELECT * FROM commit WHERE id = :commitId
        UNION ALL
        SELECT c.* FROM commit c
        INNER JOIN commit_chain cc ON c.id = cc.parent_commit_id
      )
      SELECT * FROM commit_chain ORDER BY timestamp DESC
      """, nativeQuery = true)
  List<CommitEntity> findCommitChain(@Param("commitId") UUID commitId);

  /**
   * Count commits on a branch.
   */
  long countByBranchId(UUID branchId);
}
