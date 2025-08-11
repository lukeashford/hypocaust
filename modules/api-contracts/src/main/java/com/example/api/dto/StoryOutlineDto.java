package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO representing story outline data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StoryOutlineDto(
    String title,
    String concept,
    String storyOutline,
    List<SceneDto> keyScenes,
    String tone,
    String duration
) {

}