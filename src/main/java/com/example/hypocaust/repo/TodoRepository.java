package com.example.hypocaust.repo;

import com.example.hypocaust.db.TodoEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoRepository extends JpaRepository<TodoEntity, UUID> {

  /**
   * Find root-level todos (no parent) for a given TaskExecution.
   */
  List<TodoEntity> findByTaskExecutionIdAndParentIsNull(UUID taskExecutionId);

  /**
   * Find all todos for a given TaskExecution.
   */
  List<TodoEntity> findByTaskExecutionId(UUID taskExecutionId);

  /**
   * Delete all todos for a TaskExecution.
   */
  void deleteByTaskExecutionId(UUID taskExecutionId);
}
