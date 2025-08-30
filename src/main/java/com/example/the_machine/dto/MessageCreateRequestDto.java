package com.example.the_machine.dto;

import java.util.List;
import java.util.UUID;

public record MessageCreateRequestDto(
    AuthorType author,
    List<ContentBlockDto> content,
    List<UUID> attachments  // keep for future use
) {

}