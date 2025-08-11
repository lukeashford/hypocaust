package com.example.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing an image generation prompt
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImagePromptDto(
    String prompt,
    String title,
    AssetCategory type
) {

}