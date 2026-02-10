package com.example.hypocaust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response after submitting a task")
public record TaskResponseDto(
    @Schema(description = "The project this task belongs to")
    UUID projectId,

    @Schema(description = "The ID of the created TaskExecution. Use this to subscribe to events and poll state.")
    UUID taskExecutionId,

    @Schema(
        description = "Whether the task was accepted or rejected",
        allowableValues = {"accepted", "rejected"}
    )
    String status,

    @Schema(description = "Human-readable status message")
    String message,

    @Schema(
        description = "ID of the first event (taskexecution.started) already persisted at submission time. "
            + "Pass this as Last-Event-ID when connecting to SSE to avoid replaying the started event. "
            + "Null when status is 'rejected'.",
        nullable = true
    )
    UUID firstEventId
) {

  public static TaskResponseDto accepted(UUID projectId, UUID taskExecutionId, UUID firstEventId) {
    return new TaskResponseDto(projectId, taskExecutionId, "accepted",
        "Task accepted. Subscribe to SSE events at /task-executions/" + taskExecutionId + "/events",
        firstEventId);
  }

  public static TaskResponseDto rejected(String reason) {
    return new TaskResponseDto(null, null, "rejected", reason, null);
  }
}
