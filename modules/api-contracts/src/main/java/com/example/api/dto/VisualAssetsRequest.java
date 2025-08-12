package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * DTO representing a visual assets generation request
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisualAssetsRequest(
    String brandName,
    VisualConceptsDto visualConcepts,
    StoryOutlineDto storyData
) implements Serializable {

}