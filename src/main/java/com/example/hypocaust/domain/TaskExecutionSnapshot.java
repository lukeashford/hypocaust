package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Point-in-time snapshot of a task execution's full state")
public record TaskExecutionSnapshot(
    @Schema(description = "ID of this task execution")
    UUID taskExecutionId,

    @Schema(description = "Current lifecycle status")
    TaskExecutionStatus status,

    @Schema(description = "All artifacts produced so far. "
        + "Artifacts with status GESTATING have no content yet — display a skeleton placeholder. "
        + "Artifacts with status MANIFESTED include either inline content or a URL to fetch the resource from.")
    List<Artifact> artifacts,

    @Schema(description = "Current todo / progress tree (full list, not a diff)")
    List<Todo> todos,

    @Schema(description = "ID of the most recent event. "
        + "Pass this as Last-Event-ID when (re)connecting to SSE. Null for finished executions.",
        nullable = true)
    UUID lastEventId
) {

}
