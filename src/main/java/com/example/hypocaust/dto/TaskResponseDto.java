package com.example.hypocaust.dto;

import java.util.UUID;

/**
 * Response DTO for task submission.
 *
 * @param projectId       The project ID
 * @param taskExecutionId The TaskExecution ID (for subscribing to events)
 * @param status          The status (accepted or rejected)
 * @param message         Additional message
 */
public record TaskResponseDto(
    UUID projectId,
    UUID taskExecutionId,
    String status,
    String message
) {

  public static TaskResponseDto accepted(UUID projectId, UUID taskExecutionId) {
    return new TaskResponseDto(projectId, taskExecutionId, "accepted",
        "Task accepted. Subscribe to SSE events at /task-executions/" + taskExecutionId + "/events");
  }

  public static TaskResponseDto rejected(String reason) {
    return new TaskResponseDto(null, null, "rejected", reason);
  }
}
