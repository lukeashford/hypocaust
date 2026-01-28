package com.example.hypocaust.repo;

import com.example.hypocaust.db.TaskProgressEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskProgressRepository extends JpaRepository<TaskProgressEntity, UUID> {

  /**
   * Find all task progress items for a given TaskExecution.
   */
  List<TaskProgressEntity> findByTaskExecutionIdOrderByTaskIdAsc(UUID taskExecutionId);

  /**
   * Find a specific task by TaskExecution ID and task ID.
   */
  Optional<TaskProgressEntity> findByTaskExecutionIdAndTaskId(UUID taskExecutionId, String taskId);

  /**
   * Delete all task progress for a TaskExecution.
   */
  void deleteByTaskExecutionId(UUID taskExecutionId);
}
