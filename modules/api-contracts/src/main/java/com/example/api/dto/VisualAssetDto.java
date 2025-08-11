package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a visual asset (image, etc.)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisualAssetDto(
    AssetType type,
    String url,
    String title,
    String description,
    AssetCategory category
) {

}