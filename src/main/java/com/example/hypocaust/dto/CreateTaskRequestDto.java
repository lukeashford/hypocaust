package com.example.hypocaust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Request body for submitting a new task")
public record CreateTaskRequestDto(
    @Schema(
        description = "ID of the project to run the task in",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID projectId,

    @Schema(
        description = "ID of a previous TaskExecution to continue from. "
            + "If omitted, the server uses the most recent completed execution in the project.",
        nullable = true
    )
    UUID predecessorId,

    @Schema(
        description = "Natural-language description of what the task should accomplish",
        example = "Generate a whimsical illustration of a cat astronaut",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String task,

    @Schema(
        description = "Staging batch ID from prior uploads. When provided, the server waits for "
            + "pending analyses to complete and adds the resulting artifacts to this execution's "
            + "delta. The batch is consumed and cannot be reused.",
        nullable = true
    )
    UUID batchId
) {

}
