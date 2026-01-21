package com.example.hypocaust.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provenance tracks the complete creation history of an artifact.
 * This enables smart modification: when modifying an artifact, operators can see
 * exactly how the original was created and apply targeted changes.
 *
 * @param operatorName The name of the operator that created this artifact
 * @param inputs The exact inputs used to create this artifact
 * @param runId The run that produced this artifact
 * @param derivedFrom Parent artifact IDs that informed this artifact's creation
 */
public record Provenance(
    String operatorName,
    Map<String, Object> inputs,
    UUID runId,
    List<UUID> derivedFrom
) {

  public Provenance {
    inputs = inputs != null ? Map.copyOf(inputs) : Map.of();
    derivedFrom = derivedFrom != null ? List.copyOf(derivedFrom) : List.of();
  }

  /**
   * Create a provenance record for a new artifact.
   */
  public static Provenance of(String operatorName, Map<String, Object> inputs, UUID runId) {
    return new Provenance(operatorName, inputs, runId, List.of());
  }

  /**
   * Create a provenance record for a derived artifact.
   */
  public static Provenance derived(String operatorName, Map<String, Object> inputs, UUID runId, List<UUID> parents) {
    return new Provenance(operatorName, inputs, runId, parents);
  }
}
