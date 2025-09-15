package com.example.the_machine.dto;

import java.util.List;
import java.util.UUID;

public record MessageIncomingDto(
    String content,
    List<UUID> attachments
) {

}