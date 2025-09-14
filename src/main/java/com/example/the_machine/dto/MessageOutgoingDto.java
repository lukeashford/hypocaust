package com.example.the_machine.dto;

import com.example.the_machine.db.MessageEntity;
import java.time.Instant;
import java.util.UUID;

public record MessageOutgoingDto(
    UUID id,
    Instant createdAt,
    UUID threadId,
    MessageEntity.Author author,
    String content
) {

}