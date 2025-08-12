package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * DTO representing set design in visual concepts
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SetDesignDto(
    String location,
    String description,
    List<String> props
) implements Serializable {

}