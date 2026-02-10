package com.example.hypocaust.domain;

import java.util.List;
import java.util.UUID;

public record TaskExecutionSnapshot(
    UUID taskExecutionId,
    TaskExecutionStatus status,
    List<Artifact> artifacts,
    List<Todo> todos,
    UUID lastEventId
) {

}
