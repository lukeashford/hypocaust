package com.example.hypocaust.rag;

import com.example.hypocaust.domain.ArtifactKind;
import java.util.Set;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record ModelRequirement(
    Set<ArtifactKind> inputs,
    Set<ArtifactKind> outputs,
    String tier,
    String searchString
) {

}