package com.example.hypocaust.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Point-in-time snapshot of a project's full state")
public record ProjectSnapshot(
    @Schema(description = "Readable name for this execution (e.g. 'initial_character_designs'). "
        + "Used for LLM-addressable version lookbacks.",
        nullable = true)
    String name,

    @Schema(description = "ID of the corresponding task execution",
        nullable = true)
    UUID taskExecutionId,

    @Schema(description = "Current lifecycle status",
        nullable = true)
    TaskExecutionStatus status,

    @Schema(description = "All artifacts produced so far. "
        + "Artifacts with status GESTATING have no content yet — display a skeleton placeholder. "
        + "Artifacts with status MANIFESTED include either inline content or a URL to fetch the resource from.",
        requiredMode = Schema.RequiredMode.REQUIRED)
    List<Artifact> artifacts,

    @Schema(description = "Current todo / progress tree (full list, not a diff)",
        requiredMode = Schema.RequiredMode.REQUIRED)
    List<Todo> todos,

    @Schema(description = "ID of the most recent event. "
        + "Pass this as Last-Event-ID when (re)connecting to SSE. Null for finished executions.",
        nullable = true)
    UUID lastEventId
) {

  public ProjectSnapshot() {
    this(null, null, null, List.of(), List.of(), null);
  }

  public ProjectSnapshot {
    artifacts = (artifacts == null) ? List.of() : List.copyOf(artifacts);
    todos = (todos == null) ? List.of() : List.copyOf(todos);
  }
}
