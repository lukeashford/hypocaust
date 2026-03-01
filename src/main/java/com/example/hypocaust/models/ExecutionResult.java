package com.example.hypocaust.models;

import com.example.hypocaust.domain.Artifact;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The result of a complete executor run: planning, execution, storage, and artifact finalization.
 *
 * @param artifact      the finalized artifact (MANIFESTED or FAILED)
 * @param providerInput the provider-specific input that was sent (useful for metadata/debugging)
 */
public record ExecutionResult(
    Artifact artifact,
    JsonNode providerInput
) {
}
