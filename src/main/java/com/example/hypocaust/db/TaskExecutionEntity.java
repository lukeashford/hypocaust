package com.example.hypocaust.db;

import com.example.hypocaust.domain.TaskExecutionDelta;
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
  private Status status;

  private String reason;

  private Instant startedAt;

  private Instant completedAt;

  // VERSION CONTROL FIELDS:

  /**
   * The TaskExecution this one started from (null for first).
   */
  private UUID predecessorId;

  /**
   * Auto-generated summary of changes (null if no changes).
   */
  @Column(name = "message")
  private String commitMessage;

  /**
   * What changed in this TaskExecution (null if no changes).
   */
  @Column(columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private TaskExecutionDelta delta;

  public enum Status {
    QUEUED, RUNNING, REQUIRES_ACTION, COMPLETED, FAILED, CANCELLED
  }

  public void start() {
    this.startedAt = Instant.now();
    this.status = Status.RUNNING;
  }

  public void complete(String reason) {
    this.completedAt = Instant.now();
    this.status = Status.COMPLETED;
    this.reason = reason;
  }

  /**
   * Complete with artifact changes.
   */
  public void complete(String reason, String commitMessage, TaskExecutionDelta delta) {
    this.completedAt = Instant.now();
    this.status = Status.COMPLETED;
    this.reason = reason;
    this.commitMessage = commitMessage;
    this.delta = delta;
  }

  public void fail(String reason) {
    this.completedAt = Instant.now();
    this.status = Status.FAILED;
    this.reason = reason;
  }
}
