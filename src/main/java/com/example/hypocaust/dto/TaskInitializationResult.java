package com.example.hypocaust.dto;

import java.util.UUID;

/**
 * Result of task initialization, containing all IDs needed for orchestration.
 */
public record TaskInitializationResult(
    UUID projectId,
    UUID taskExecutionId,
    UUID predecessorId,
    UUID firstEventId
) {

}
