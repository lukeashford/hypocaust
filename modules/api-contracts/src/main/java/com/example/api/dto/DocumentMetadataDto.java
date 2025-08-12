package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * DTO representing document metadata
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentMetadataDto(
    int pages,
    String size,
    String format
) implements Serializable {

}