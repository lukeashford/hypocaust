package com.example.hypocaust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Summary of a single task execution")
public record TaskExecutionDto(
    @Schema(description = "Unique execution ID")
    UUID id,

    @Schema(description = "Project this execution belongs to")
    UUID projectId,

    @Schema(description = "The task description that was submitted")
    String task,

    @Schema(description = "Current status of the execution")
    RunStatus status,

    @Schema(description = "Failure reason or commit message (null while running)", nullable = true)
    String reason,

    @Schema(description = "When the execution started running", nullable = true)
    Instant startedAt,

    @Schema(description = "When the execution finished (completed or failed)", nullable = true)
    Instant completedAt
) {

}
