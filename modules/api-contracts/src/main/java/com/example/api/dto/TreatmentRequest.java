package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * DTO representing a treatment document generation request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TreatmentRequest(
    String brandName,
    CompanyAnalysisDto companyData,
    StoryOutlineDto storyData,
    VisualConceptsDto visualConcepts,
    List<VisualAssetDto> assets
) implements Serializable {

}