package com.example.hypocaust.db;

import com.example.hypocaust.domain.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a task item in the progress tree.
 * Links to a TaskExecutionEntity and stores hierarchical task progress.
 */
@Entity
@Table(name = "task_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProgressEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID taskExecutionId;

  /**
   * Hierarchical identifier (e.g., "0", "0.1", "0.1.2").
   */
  @Column(nullable = false)
  private String taskId;

  /**
   * Human-readable description (the "todo" wording from ledger).
   */
  @Column(nullable = false, length = 500)
  private String description;

  /**
   * Current status of the task.
   */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TaskStatus status;
}
