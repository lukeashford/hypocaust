package com.example.the_machine.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageDTO(
    UUID id,
    UUID threadId,
    AuthorType author,
    Instant createdAt,
    List<ContentBlock> content,
    List<UUID> attachments
) {

}