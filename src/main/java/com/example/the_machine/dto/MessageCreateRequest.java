package com.example.the_machine.dto;

import java.util.List;
import java.util.UUID;

public record MessageCreateRequest(
    AuthorType author,
    List<ContentBlock> content,
    List<UUID> attachments  // keep for future use
) {

}