package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * The result of a complete executor run: planning, execution, storage, and artifact finalization.
 *
 * @param artifacts the finalized artifacts (MANIFESTED or FAILED)
 * @param providerInput the provider-specific input that was sent (useful for metadata/debugging)
 */
public record ExecutionResult(
    List<Artifact> artifacts,
    JsonNode providerInput
) {

}
