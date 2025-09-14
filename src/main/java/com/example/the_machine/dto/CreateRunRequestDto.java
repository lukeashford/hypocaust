package com.example.the_machine.dto;

import java.util.UUID;

public record CreateRunRequestDto(
    UUID threadId,
    UUID assistantId,
    String task
) {

}