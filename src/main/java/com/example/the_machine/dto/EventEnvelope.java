package com.example.the_machine.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record EventEnvelope(
    String type,
    // run.created|run.updated|message.delta|message.completed|artifact.created|error
    UUID threadId,
    UUID runId,
    UUID messageId,
    JsonNode data
) {

}