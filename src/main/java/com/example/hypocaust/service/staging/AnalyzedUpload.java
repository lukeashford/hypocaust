package com.example.hypocaust.service.staging;

import com.example.hypocaust.domain.ArtifactKind;
import com.example.hypocaust.service.analysis.AnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record AnalyzedUpload(
    UUID dataPackageId,
    String storageKey,
    JsonNode inlineContent,
    String originalFilename,
    String mimeType,
    ArtifactKind kind,
    String clientName,
    String clientTitle,
    String clientDescription,
    AnalysisResult analysisResult
) {

}
