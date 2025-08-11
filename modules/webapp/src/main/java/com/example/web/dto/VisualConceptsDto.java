package com.example.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * DTO representing visual concepts data
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisualConceptsDto(
    List<CharacterDto> characters,
    List<String> colorPalette,
    String lightingStyle,
    List<SetDesignDto> setDesign,
    String visualEffects,
    String productPlacement
) {

}