package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of a todo / progress item", enumAsRef = true)
public enum TodoStatus {
  PENDING,
  IN_PROGRESS,
  COMPLETED,
  FAILED,
  CANCELLED
}
