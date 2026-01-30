package com.example.hypocaust.domain;

/**
 * Records a change to an artifact within a TaskExecution. Used in TaskExecutionDelta to track what
 * was added or edited.
 *
 * @param name The semantic artifact fileName (e.g., "protagonists_dog")
 */
public record ArtifactChange(String name) {

}
