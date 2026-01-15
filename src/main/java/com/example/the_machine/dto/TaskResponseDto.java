package com.example.the_machine.dto;

import java.util.UUID;

public record TaskResponseDto(
    UUID projectId,
    String status,
    String message
) {

  public static TaskResponseDto accepted(UUID projectId) {
    return new TaskResponseDto(projectId, "accepted", "Task accepted. Subscribe to SSE events at /projects/" + projectId + "/events");
  }

  public static TaskResponseDto rejected(String reason) {
    return new TaskResponseDto(null, "rejected", reason);
  }
}
