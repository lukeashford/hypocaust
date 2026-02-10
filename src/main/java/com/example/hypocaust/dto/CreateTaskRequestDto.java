package com.example.hypocaust.dto;

import java.util.UUID;

/**
 * Request DTO for creating a new task.
 *
 * @param projectId     The project ID (required)
 * @param predecessorId The TaskExecution to start from (optional, defaults to most recent)
 * @param task          The task description (required)
 */
public record CreateTaskRequestDto(
    UUID projectId,
    UUID predecessorId,
    String task
) {

}
