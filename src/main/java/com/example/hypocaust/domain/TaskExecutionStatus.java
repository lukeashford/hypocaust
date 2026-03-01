package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a task execution", enumAsRef = true)
public enum TaskExecutionStatus {
  QUEUED, RUNNING, REQUIRES_ACTION, COMPLETED, PARTIALLY_SUCCESSFUL, FAILED, CANCELLED
}
