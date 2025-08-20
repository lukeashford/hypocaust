package com.example.the_machine.dto;

import java.util.List;

public record ThreadViewDTO(
    ThreadDTO thread,
    List<MessageDTO> messages,
    List<ArtifactDTO> artifacts,
    RunDTO latestRun
) {

}