package com.example.the_machine.dto;

import java.util.UUID;

public record CreateRunRequest(
    UUID threadId,
    UUID assistantId,            // nullable -> use default assistant
    MessageCreateRequest input   // nullable -> run without new user message
) {

}