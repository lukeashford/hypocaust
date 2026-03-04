package com.example.hypocaust.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record ArtifactIntent(
    @JsonProperty("action") ArtifactAction action,
    @JsonProperty("kind") ArtifactKind kind,
    @JsonProperty("targetName") String targetName,
    @JsonProperty("description") String description,
    @JsonProperty("executionName") String executionName
) {

}
