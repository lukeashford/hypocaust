package com.example.hypocaust.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record ArtifactIntent(
    @JsonProperty("action")
    @JsonPropertyDescription("The action to perform: ADD (create new), EDIT (modify existing), "
        + "DELETE (remove), or RESTORE (bring back from history)")
    ArtifactAction action,

    @JsonProperty("kind")
    @JsonPropertyDescription("Content type of the artifact (ADD and EDIT only): "
        + "IMAGE, AUDIO, VIDEO, TEXT, PDF, or OTHER")
    ArtifactKind kind,

    @JsonProperty("targetName")
    @JsonPropertyDescription("Name of the existing artifact to operate on (EDIT, DELETE, RESTORE)")
    String targetName,

    @JsonProperty("description")
    @JsonPropertyDescription("Concise one-sentence summary of what the artifact contains or "
        + "what the edit changes (ADD, EDIT)")
    String description,

    @JsonProperty("executionName")
    @JsonPropertyDescription("Task execution name to restore from, e.g. 'initial_character_designs' "
        + "(RESTORE only)")
    String executionName,

    @JsonProperty("preferredName")
    @JsonPropertyDescription("Short snake_case identifier for the artifact, max 30 characters, "
        + "e.g. 'cat_astronaut' (ADD only)")
    String preferredName,

    @JsonProperty("preferredTitle")
    @JsonPropertyDescription("Human-readable title, e.g. 'Cat Astronaut Illustration' (ADD only)")
    String preferredTitle
) {

}
