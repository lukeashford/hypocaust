package com.example.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;
import java.util.List;

/**
 * DTO representing visual concepts including characters, color palette, and set design
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisualConceptsDto(
    List<CharacterDto> characters,
    List<String> colorPalette,
    String lightingStyle,
    List<SetDesignDto> setDesign,
    String visualEffects,
    String productPlacement
) implements Serializable {

}