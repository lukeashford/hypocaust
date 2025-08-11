package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO representing set design information
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SetDesignDto(
    String location,
    String description,
    List<String> props
) {

}