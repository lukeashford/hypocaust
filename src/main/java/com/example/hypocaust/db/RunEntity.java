package com.example.hypocaust.db;

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

@Entity
@Table(name = "run")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RunEntity extends BaseEntity {

  @Column(nullable = false)
  private UUID projectId;

  private String task;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  private String reason;

  private Instant startedAt;

  private Instant completedAt;

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

  public void fail(String reason) {
    this.completedAt = Instant.now();
    this.status = Status.FAILED;
    this.reason = reason;
  }
}