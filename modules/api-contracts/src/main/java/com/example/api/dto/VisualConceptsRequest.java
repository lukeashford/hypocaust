package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * DTO representing a visual concepts generation request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisualConceptsRequest(
    @NotBlank(message = "Brand name is required")
    String brandName,

    @Valid
    StoryOutlineDto storyData,

    @Valid
    CompanyAnalysisDto companyData
) implements Serializable {

}