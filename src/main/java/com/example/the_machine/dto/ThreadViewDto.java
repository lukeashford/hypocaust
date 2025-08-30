package com.example.the_machine.dto;

import java.util.List;

public record ThreadViewDto(
    ThreadDto thread,
    List<MessageDto> messages,
    List<ArtifactDto> artifacts,
    RunDto latestRun
) {

}