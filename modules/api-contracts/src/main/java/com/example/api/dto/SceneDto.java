package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO representing a scene in a story outline
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SceneDto(
    int sceneNumber,
    String location,
    String description,
    String visualNotes
) {

}