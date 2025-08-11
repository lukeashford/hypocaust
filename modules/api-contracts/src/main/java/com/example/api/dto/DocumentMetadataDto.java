package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing document metadata information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentMetadataDto(
    int pages,
    String size,
    String format
) {

}