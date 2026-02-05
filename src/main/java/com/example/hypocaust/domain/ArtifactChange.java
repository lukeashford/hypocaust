package com.example.hypocaust.domain;

import java.util.UUID;

/**
 * Records a change to an artifact within a TaskExecution. Used in TaskExecutionDelta to track what
 * was added or edited.
 *
 * @param name The semantic artifact name (e.g., "protagonists_dog")
 * @param artifactId The unique ID of the ArtifactEntity
 */
public record ArtifactChange(String name, UUID artifactId) {

}
