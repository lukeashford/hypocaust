package com.example.hypocaust.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request body for creating a new project")
public record CreateProjectRequestDto(
    @Schema(
        description = "Unique human-readable name for the project",
        example = "my-cool-project",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 100
    )
    String name
) {

}
