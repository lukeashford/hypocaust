package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a character in visual concepts
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CharacterDto(
    String name,
    String description,
    String costume,
    String visualNotes
) {

}