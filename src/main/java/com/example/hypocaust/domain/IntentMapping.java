package com.example.hypocaust.domain;

import lombok.Builder;

@Builder
public record IntentMapping(
    ArtifactIntent intent,
    OutputSpec outputSpec
) {

}
