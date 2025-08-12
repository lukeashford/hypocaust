package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * DTO representing a story generation request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryRequest(
    String brandName,
    CompanyAnalysisDto companyData
) implements Serializable {

}