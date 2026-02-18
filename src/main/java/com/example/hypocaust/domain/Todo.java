package com.example.hypocaust.domain;

import com.github.f4b6a3.uuid.UuidCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;

@Builder(toBuilder = true)
@Schema(description = "A single item in the hierarchical progress / todo tree")
public record Todo(
    @Schema(description = "Unique todo ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id,

    @Schema(description = "Human-readable description of this step", example = "Generate color palette", requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull String description,

    @Schema(description = "Current status of this step", requiredMode = Schema.RequiredMode.REQUIRED)
    @NonNull TodoStatus status,

    @Schema(description = "Nested child steps (empty list if leaf node)")
    @NonNull List<Todo> children
) {

  public Todo {
    if (id == null) {
      id = UuidCreator.getTimeOrderedEpoch();
    }
  }

  public Todo(String description, TodoStatus status) {
    this(null, description, status, List.of());
  }

  public List<Todo> children() {
    return children;
  }
}
