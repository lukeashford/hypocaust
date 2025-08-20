package com.example.the_machine.dto;

import java.time.Instant;
import java.util.UUID;

public record ThreadDTO(
    UUID id,
    String title,
    Instant createdAt,
    Instant lastActivityAt
) {

}