package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO representing company analysis data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CompanyAnalysisDto(
    String summary,
    List<String> keyPoints,
    String brandPersonality,
    String targetAudience,
    String visualStyle,
    List<String> keyMessages,
    List<String> competitiveAdvantages
) {

}