package com.example.hypocaust.domain;

import java.util.List;

/**
 * Records what changed in a TaskExecution. Only present if there were artifact changes.
 *
 * @param added New artifacts created in this TaskExecution
 * @param edited New versions of existing artifacts
 * @param deleted Artifact names that were marked as deleted
 */
public record TaskExecutionDelta(
    List<ArtifactChange> added,
    List<ArtifactChange> edited,
    List<String> deleted
) {

  public TaskExecutionDelta() {
    this(List.of(), List.of(), List.of());
  }

  public boolean hasChanges() {
    return !added.isEmpty() || !edited.isEmpty() || !deleted.isEmpty();
  }
}
