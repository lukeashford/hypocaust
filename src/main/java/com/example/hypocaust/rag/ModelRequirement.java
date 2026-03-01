package com.example.hypocaust.rag;

import com.example.hypocaust.domain.ArtifactKind;
import java.util.Set;

public record ModelRequirement(
    Set<ArtifactKind> inputs,
    ArtifactKind output,
    String tier,
    String searchString
) {

}