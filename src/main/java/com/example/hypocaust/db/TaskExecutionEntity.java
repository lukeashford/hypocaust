package com.example.hypocaust.db;

import com.example.hypocaust.domain.TaskExecutionDelta;
import com.example.hypocaust.domain.TaskExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "task_execution")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TaskExecutionEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID projectId;

  private String task;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private TaskExecutionStatus status = TaskExecutionStatus.QUEUED;

  private Instant startedAt;

  private Instant completedAt;

  // VERSION CONTROL FIELDS:

  /**
   * The TaskExecution this one started from (null for first).
   */
  private UUID predecessorId;

  /**
   * LLM-generated readable name for this execution (e.g. "initial_character_designs"). Used for
   * LLM-addressable version lookbacks instead of UUIDs.
   */
  @Column(length = 50)
  private String name;

  /**
   * Auto-generated summary of changes (null if no changes).
   */
  private String commitMessage;

  /**
   * What changed in this TaskExecution (null if no changes).
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private TaskExecutionDelta delta;

  public void start() {
    this.startedAt = Instant.now();
    this.status = TaskExecutionStatus.RUNNING;
  }

  /**
   * Complete with optional artifact changes.
   *
   * @param commitMessage Summary of what was done (LLM-generated for success)
   * @param delta The changes delta (null if no artifact changes)
   */
  public void complete(String commitMessage, TaskExecutionDelta delta) {
    this.completedAt = Instant.now();
    this.status = TaskExecutionStatus.COMPLETED;
    this.commitMessage = commitMessage;
    this.delta = delta;
  }

  /**
   * Complete with partial failures.
   *
   * @param commitMessage Summary of what was done
   * @param delta The changes delta (including failed artifacts)
   */
  public void partiallySuccessful(String commitMessage, TaskExecutionDelta delta) {
    this.completedAt = Instant.now();
    this.status = TaskExecutionStatus.PARTIALLY_SUCCESSFUL;
    this.commitMessage = commitMessage;
    this.delta = delta;
  }

  /**
   * Mark as failed with error message.
   *
   * @param commitMessage The error message describing why it failed
   */
  public void fail(String commitMessage) {
    this.completedAt = Instant.now();
    this.status = TaskExecutionStatus.FAILED;
    this.commitMessage = commitMessage;
  }
}
