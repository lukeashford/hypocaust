package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing treatment document data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TreatmentDocumentDto(
    String title,
    String executiveSummary,
    String creativeStrategy,
    String storyBreakdown,
    String visualDirection,
    String productionNotes,
    String castingNotes,
    String locationRequirements,
    String technicalSpecifications,
    String budgetConsiderations,
    String timeline,
    String postProductionNotes,
    String brandName,
    String generatedAt,
    Integer totalAssets,
    DocumentMetadataDto documentMetadata
) {

}