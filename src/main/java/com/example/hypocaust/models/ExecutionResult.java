package com.example.hypocaust.models;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The result of a complete executor run: planning, execution, and output extraction.
 *
 * @param output        the extracted output (URL, text content, etc.)
 * @param providerInput the provider-specific input that was sent (useful for metadata/debugging)
 */
public record ExecutionResult(
    String output,
    JsonNode providerInput
) {
}
