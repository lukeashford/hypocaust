package com.example.hypocaust.repo;

import com.example.hypocaust.db.TaskExecutionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, UUID> {

  /**
   * Find the most recent TaskExecution for a project by start time.
   */
  Optional<TaskExecutionEntity> findTopByProjectIdOrderByStartedAtDesc(UUID projectId);

  /**
   * Find the most recent completed TaskExecution for a project.
   */
  @Query("SELECT te FROM TaskExecutionEntity te WHERE te.projectId = :projectId AND te.status = 'COMPLETED' ORDER BY te.completedAt DESC LIMIT 1")
  Optional<TaskExecutionEntity> findMostRecentCompletedByProjectId(@Param("projectId") UUID projectId);

  /**
   * Find all TaskExecutions for a project, ordered by creation time.
   */
  List<TaskExecutionEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

  /**
   * Find all TaskExecutions for a project that have changes (non-null delta).
   */
  @Query("SELECT te FROM TaskExecutionEntity te WHERE te.projectId = :projectId AND te.delta IS NOT NULL ORDER BY te.createdAt DESC")
  List<TaskExecutionEntity> findWithChangesByProjectId(@Param("projectId") UUID projectId);

  /**
   * Find TaskExecutions that have a specific predecessor.
   */
  List<TaskExecutionEntity> findByPredecessorId(UUID predecessorId);
}
