package com.example.the_machine.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageDto(
    UUID id,
    UUID threadId,
    AuthorType author,
    Instant createdAt,
    List<ContentBlockDto> content,
    List<UUID> attachments
) {

}