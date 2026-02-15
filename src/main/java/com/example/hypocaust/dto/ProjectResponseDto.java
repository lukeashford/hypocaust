package com.example.hypocaust.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Response after creating a project")
public record ProjectResponseDto(
    @Schema(description = "The unique project ID", example = "01963d1e-7b1a-7f00-a123-456789abcdef")
    UUID id,

    @Schema(description = "The unique project name", example = "my-cool-project")
    String name
) {

}
