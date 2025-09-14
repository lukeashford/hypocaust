package com.example.the_machine.dto;

import java.util.List;

public record ThreadViewDto(
    ThreadDto thread,
    List<MessageOutgoingDto> messages,
    List<ArtifactDto> artifacts,
    RunDto latestRun
) {

}