package com.example.the_machine.dto;

import java.util.UUID;

public record CreateRunRequestDto(
    UUID threadId,
    UUID assistantId,            // nullable -> use default assistant
    MessageCreateRequestDto input   // nullable -> run without new user message
) {

}