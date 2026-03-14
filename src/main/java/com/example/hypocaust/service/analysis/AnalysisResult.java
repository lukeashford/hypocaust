package com.example.hypocaust.service.analysis;

import com.fasterxml.jackson.databind.JsonNode;

public record AnalysisResult(String name, String title, String description,
    JsonNode enrichedMetadata) {

}
